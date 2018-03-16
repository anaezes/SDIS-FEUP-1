package Peer;

import Common.remote.IControl;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer extends Thread implements IControl {

    private int peerId;

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
        peer.peerId = Integer.parseInt(args[0]);
        System.out.println("Peer Id: " + peer.peerId);

        try {
            IControl control = (IControl) UnicastRemoteObject.exportObject(peer, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello", control);

            System.err.println("Server ready");
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

    @Override
    public String Backup() throws RemoteException {
        return "Hello World!";
    }
}