package Peer;

import Common.remote.IControl;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

public class Peer extends Thread implements IControl {
    private final String FILES_DIRECTORY = System.getProperty("user.dir") + "/filesystem/peers/peer";

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
        }
    }

    public void handleDataChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                System.out.println("Data Channel waiting...");

                mdbSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Data Channel received: "+ new String(packet.getData(), 0, packet.getLength()));
        }
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

    private void sendMessage(String message) {

        System.err.println("Send message...\n");

        try {
            byte[] bfr = message.getBytes();
            DatagramPacket packet = new DatagramPacket(bfr, bfr.length, InetAddress.getByAddress(mcAddr.getAddress()), mcPort);
            mcSocket.send(packet);
        } catch(SocketException e){
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
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
        Files.createDirectory(path);
    }

    @Override
    public String backup(String fileContent, String fileName, int replicationDegree) throws RemoteException {

        System.out.println("RECEBI ::::");
        System.out.println(fileContent);

        try {
            Files.write(Paths.get(getFileSystemPath()+"/"+fileName), fileContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String request = "Hello world !!! :)";
        this.sendMessage(request);
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
}