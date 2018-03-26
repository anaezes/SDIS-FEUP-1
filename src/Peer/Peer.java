package Peer;

import Common.messages.Message;
import Common.messages.PutChunkMessage;
import Common.messages.StoredMessage;
import Common.messages.Version;
import Common.remote.IControl;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Peer extends Thread implements IControl {
    private final String FILES_DIRECTORY = System.getProperty("user.dir") + "/filesystem/peers/peer";
    private final int CHUNKSIZE = 64000;

    private final int peerId;

    // Control channel
    private MulticastSocket mcSocket;
    private InetAddress mcAddr;
    private int mcPort;

    // Data channel
    private MulticastSocket mdbSocket;
    private InetAddress mdbAddr;
    private int mdbPort;

    // Data recovery channel
    private MulticastSocket mdrSocket;
    private InetAddress mdrAddr;
    private int mdrPort;

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Usage: java Peer <Peer_id> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>");
            return;
        }

        Peer peer = new Peer(args);
        System.out.println("Peer Id: " + peer.getPeerId());

        try {
            //verify if this peer base directory exists. If not creates it.
            peer.checkFileSystem();

            Registry registry;
            // Bind the remote object's stub in the registry
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (ExportException e) {
                if (e.toString().contains("Port already in use")) {
                    registry = LocateRegistry.getRegistry();
                } else {
                    throw e;
                }
            }
            IControl control = (IControl) UnicastRemoteObject.exportObject(peer, 0);
            registry.bind( "peer" + peer.getPeerId(), control);
            System.err.println("Server ready\n");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        peer.start();
    }

    public Peer(String[] args) {

        initControlChannel(args[1], args[2]);
        initDataChannel(args[3], args[4]);
        initRecoveryChannel(args[5], args[6]);

        this.peerId = Integer.parseInt(args[0]);
    }

    private void initControlChannel(String address, String Port) {
        try {
            mcAddr = InetAddress.getByName(address);
            mcPort = Integer.parseInt(Port);

            mcSocket = new MulticastSocket(mcPort);
            mcSocket.joinGroup(mcAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e){
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    private void initDataChannel(String address, String Port) {
        try {
            mdbAddr = InetAddress.getByName(address);
            mdbPort = Integer.parseInt(Port);

            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e){
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    private void initRecoveryChannel(String address, String Port) {
        try {
            mdrAddr = InetAddress.getByName(address);
            mdrPort = Integer.parseInt(Port);

            mdrSocket = new MulticastSocket(mdrPort);
            mdrSocket.joinGroup(mdrAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        }
    }


    public void start() {
        new Thread(() -> handleControlChannel()).start();
        new Thread(() -> handleDataChannel()).start();
        new Thread(() -> handleDataRecoveryChannel()).start();
    }

    public void handleControlChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                System.out.println("Control Channel waiting...");
                mcSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Control Channel received: "+ new String(packet.getData(), 0, packet.getLength()));
            // TODO: send stored message after the delay 0-400ms
        }
    }

    public void handleDataChannel() {

        byte[] buffer = new byte[CHUNKSIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                System.out.println("Data Channel waiting...");
                mdbSocket.receive(packet);

                String value = new String(packet.getData(), "UTF-8");
                String[] parameters = value.split(" ");
                String[] headerBody = value.split("\r\n\r\n");

                if(parameters[0].equals("PUTCHUNK"))
                    handlePutChunkMessage(parameters, headerBody[1]);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

/*
* Function that creates root directory, if non-existent, and stores the received chunk
* */
    private void handlePutChunkMessage(String[] parameters, String body) throws IOException {

        if(Integer.parseInt(parameters[2]) == peerId)
            return;

        Path path = Paths.get(getFileSystemPath() + "/" + parameters[3]);
        if (!Files.exists(path))
            Files.createDirectory(path);
        Files.write(Paths.get(path.toString() + "/" + parameters[4]), body.getBytes());

        //send message STORED chunk
        String[] version = parameters[1].split("\\.");
        StoredMessage message = new StoredMessage(new Version(Integer.parseInt(version[0]), Integer.parseInt(version[1])),
                Integer.parseInt(parameters[2]), parameters[3], Integer.parseInt(parameters[4]));
        sendMessage(message);
    }

    public void handleDataRecoveryChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                System.out.println("Data Recovery Channel waiting...");

                mdrSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Data Recovery Channel received: "+ new String(packet.getData(), 0, packet.getLength()));
        }
    }

    private void sendMessage(Message message) throws IOException {
        System.err.println("Send message " + message.getMessageType() + "\n");
        DatagramPacket packet;

        switch (message.getMessageType()) {
            case PUTCHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(mdbAddr.getAddress()), mdbPort);
                mdbSocket.send(packet);
                break;
            case STORED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(mcAddr.getAddress()), mcPort);
                mdbSocket.send(packet);
                break;
        }
    }

    public int getPeerId() {
        return this.peerId;
    }

    public String getFileSystemPath() {
        return FILES_DIRECTORY + this.peerId;
    }

    public void checkFileSystem() throws IOException {
        Path path = Paths.get(this.getFileSystemPath());
        if (!Files.exists(path))
            Files.createDirectory(path);
    }

    @Override
    public String backup(byte[] fileContent, String fileName, String lastModification, int replicationDegree) throws RemoteException {

        try {
            byte fileChunks[][] = getFileChunks(fileContent);
            int i = 0;
           while(i < fileChunks.length) {

            PutChunkMessage message = new PutChunkMessage(new Version(1, 0), peerId, getEncodeHash(fileName+lastModification), i, replicationDegree, fileChunks[i]);
               this.sendMessage(message);
               i++;
           }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Operation backup...";
    }

    @Override
    public String delete() throws RemoteException {
        return "Operation delete...";
    }

    @Override
    public String restore() throws RemoteException {
        return "Operation restore...";
    }

    @Override
    public String reclaim() throws RemoteException {
        return "Operation reclaim...";
    }

    /*
    * Main function for split file in array of chunks
    **/
    private byte[][] getFileChunks(byte[] fileContent) {
        byte buffer[][] = new byte[fileContent.length/CHUNKSIZE+1][CHUNKSIZE];

        int initialPos = 0;
        int lastPos = CHUNKSIZE;

        for(int i = 0; i < buffer.length; i++) {
            buffer[i] = getChunk(fileContent, initialPos, lastPos);
            initialPos += CHUNKSIZE;
            lastPos += CHUNKSIZE;

        }

        return buffer;
    }

    /*
    * Split each chunk
    **/
    private byte[] getChunk(byte[] fileContent, int initPos, int lastPos) {
        byte[] chunk = new byte[CHUNKSIZE];

        int i = 0;
        while(initPos < lastPos && initPos < fileContent.length ){
            chunk[i] = fileContent[initPos];
            i++; initPos++;
        }
        return chunk;
    }

    private String getEncodeHash(String text)  {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(hash.length);
        for(byte b : hash)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}