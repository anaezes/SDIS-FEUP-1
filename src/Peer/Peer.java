package Peer;

import Common.messages.*;
import Common.remote.IControl;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;

public class Peer extends Thread implements IControl {
    private static final int NUMBER_TRIES = 3;
    private final String FILES_DIRECTORY = System.getProperty("user.dir") + File.separator +"filesystem" +File.separator
            + "peers" + File.separator +"peer";
    private final String CLIENT_DIRECTORY = System.getProperty("user.dir") + File.separator + "filesystem" +
            File.separator + "client" + File.separator;

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

    //store received ACKs
    private final HashMap<String, HashSet<Integer>> acks = new HashMap<>();

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

    /**
    * Constructor of Peer
    */
    public Peer(String[] args) {

        initControlChannel(args[1], args[2]);
        initDataChannel(args[3], args[4]);
        initRecoveryChannel(args[5], args[6]);

        this.peerId = Integer.parseInt(args[0]);
    }

    /**
    * Init channel for control messages
    * @param address
    * @param port
    */
    private void initControlChannel(String address, String port) {
        try {
            mcAddr = InetAddress.getByName(address);
            mcPort = Integer.parseInt(port);

            mcSocket = new MulticastSocket(mcPort);
            mcSocket.joinGroup(mcAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e){
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    /**
    * Init channel for data messages
    * @param address
    * @param port
    */
    private void initDataChannel(String address, String port) {
        try {
            mdbAddr = InetAddress.getByName(address);
            mdbPort = Integer.parseInt(port);

            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e){
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    /**
    * Init channel for recovery messages
    * @param address
    * @param port
    */
    private void initRecoveryChannel(String address, String port) {
        try {
            mdrAddr = InetAddress.getByName(address);
            mdrPort = Integer.parseInt(port);

            mdrSocket = new MulticastSocket(mdrPort);
            mdrSocket.joinGroup(mdrAddr);

        } catch (UnknownHostException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    /*
    * Creates threads that wait for messages from channels
    * */
    public void start() {
        new Thread(() -> handleControlChannel()).start();
        new Thread(() -> handleDataChannel()).start();
        new Thread(() -> handleDataRecoveryChannel()).start();
    }

    /**
    * Handle control channel messages
    */
    public void handleControlChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                mcSocket.receive(packet);

                Message message = Message.parseMessage(packet);

                System.out.println("\nReceived message on MC Channel: " + message.getMessageType() + "\n");

                if(message instanceof StoredMessage) {
                    //store sent ack
                    HashSet<Integer> set = acks.getOrDefault(message.getFileId(), new HashSet<>());
                    set.add(message.getChunkNo());
                    acks.putIfAbsent(message.getFileId(), set);
                }
                else if(message instanceof DeleteMessage){

                    handleDeleteMessage((DeleteMessage) message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDeleteMessage(DeleteMessage message) throws IOException {
        if(message.getSenderId() == peerId)
            return;

        File file = new File(getFileSystemPath() + "/" + message.getFileId());

        if(!file.exists())
            return;

        if(!deleteFile(file))
            System.out.println("Error to delete a file! ");
    }

    private boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFile(children[i]);
                if (!success) {
                    return false;
                }
            }
        }

        return file.delete();
    }


    /**
    * Handle data channel messages
    */
    public void handleDataChannel() {

        byte[] buffer = new byte[CHUNKSIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                mdbSocket.receive(packet);

                Message message = Message.parseMessage(packet);
                System.out.println("\nReceived message on MDB Channel: " + message.getMessageType() + "\n");

                if(message instanceof PutChunkMessage)
                    handlePutChunkMessage((PutChunkMessage) message);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
    * Handle data recovery channel messages
    */
    public void handleDataRecoveryChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                mdrSocket.receive(packet);
               // System.out.println("\nReceived message on MC Channel: " + message.getMessageType() + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Data Recovery Channel received: "+ new String(packet.getData(), 0, packet.getLength()));
        }
    }

    /**
    * Creates root directory, if non-existent, and stores the received chunk
    * @param message
    */
    private void handlePutChunkMessage(PutChunkMessage message) throws IOException {

        if(message.getSenderId() == peerId)
            return;

        Path path = Paths.get(getFileSystemPath() + "/" + message.getFileId());
        if (!Files.exists(path))
            Files.createDirectory(path);
        Files.write(Paths.get(path.toString() + "/" + message.getChunkNo()), message.getBody());

        //send message STORED chunk
        StoredMessage storedMessage = new StoredMessage(message.getVersion(), peerId, message.getFileId(),
                message.getChunkNo());
        sendMessage(storedMessage);
    }


    /**
    * Sends message according to its type
    * @param message
    */
    private void sendMessage(Message message) throws IOException {
        System.err.println("\nSend message " + message.getMessageType() + "\n");
        DatagramPacket packet;

        switch (message.getMessageType()) {
            case PUTCHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(mdbAddr.getAddress()), mdbPort);
                mdbSocket.send(packet);
                break;
            case STORED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(mcAddr.getAddress()), mcPort);
                mcSocket.send(packet);
                break;
            case DELETE:
                //3 times
                for(int i = 0; i < NUMBER_TRIES; i++) {
                    packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByAddress(mcAddr.getAddress()), mcPort);
                    mcSocket.send(packet);
                }
                break;
        }
    }

    /**
    * Get peer id
    * @return peerId
    */
    public int getPeerId() {
        return this.peerId;
    }

    /**
    * Get filesystem root path
    * @return path
    */
    public String getFileSystemPath() {
        return FILES_DIRECTORY + this.peerId;
    }

    /**
    * Checks if peer directory exist, if non-existent create.
    */
    public void checkFileSystem() throws IOException {
        Path path = Paths.get(this.getFileSystemPath());
        if (!Files.exists(path))
            Files.createDirectory(path);
    }

    /**
    * Receive file, split it in chunks and send them to the other peers
    * @param file
    * @param replicationDegree
    * @return name of operation //todo(?)
    */
    @Override
    public String backup(File file, int replicationDegree) throws RemoteException {

        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(CLIENT_DIRECTORY+file.getName())));
            int timeout = 200;
            int numberOfTries = 3;
            backupHandle(fileContent.getBytes(),  file.getName(),  Long.toString(file.lastModified()), replicationDegree, timeout, numberOfTries);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return "Operation backup...";
    }

    private void backupHandle(byte[] fileContent, String fileName, String lastModification,
                              int replicationDegree, int timeout, int numberOfTries) throws IOException, InterruptedException {

        if(numberOfTries == 0)
            return;

        byte fileChunks[][] = getFileChunks(fileContent);
        int chunks[] = new int[fileChunks.length];
        int i = 0;
        String fileId = getEncodeHash(fileName+lastModification);

        while(i < fileChunks.length) {
            PutChunkMessage message = new PutChunkMessage(new Version(1, 0), peerId, fileId, i, replicationDegree, fileChunks[i]);
            this.sendMessage(message);
            chunks[i] = i;
            i++;
        }

        sleep(timeout);

        timeout = timeout*2;
        numberOfTries--;

        //resend chunks if failure
        if(chunks.length < replicationDegree) {
            backupHandle(fileContent, fileName, lastModification, replicationDegree, timeout, numberOfTries);
        }
    }

    @Override
    public String delete(File file) throws RemoteException {

        try {
            String fileId = getEncodeHash(file.getName()+file.lastModified());
            DeleteMessage message = new DeleteMessage(new Version(1, 0), peerId, fileId);
            this.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Operation delete...";
    }

    @Override
    public String restore(File file) throws RemoteException {

        //todo

        return "Operation restore...";
    }

    @Override
    public String reclaim() throws RemoteException {

        //todo

        return "Operation reclaim...";
    }

    /**
    * Split file in array of chunks
    * @param fileContent
    * @return array of byte array with all chunks of fileContent
    */
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

    /**
    * Get chunk from a given position
    * @param fileContent
    * @param initPos
    * @param lastPos
    * @return byte array with chunk
    */
    private byte[] getChunk(byte[] fileContent, int initPos, int lastPos) {
        byte[] chunk = new byte[CHUNKSIZE];

        int i = 0;
        while(initPos < lastPos && initPos < fileContent.length ){
            chunk[i] = fileContent[initPos];
            i++; initPos++;
        }
        return chunk;
    }

    /**
    * Get file id encoded
    * @param text
    * @return text encoded
    */
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

    private void checkChunksStored(int[] chunks, String fileId, byte[][] fileChunks, int replicationDegree) throws IOException {
        HashSet<Integer> set = acks.get(fileId);

        System.out.println("verify if all is stored...");

        for(int i = 0; i < chunks.length; i++) {
           if(!set.contains(chunks[i])) {
               PutChunkMessage message = new PutChunkMessage(new Version(1, 0), peerId, fileId, chunks[i], replicationDegree, fileChunks[i]);
               this.sendMessage(message);

               System.out.println("resend...");
           }
        }
    }
}