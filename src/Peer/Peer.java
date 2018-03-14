package Peer;

import java.io.IOException;
import java.net.*;

public class Peer extends Thread{

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

        Peer Peer = new Peer(args);

        Peer.peerId = Integer.parseInt(args[0]);
        System.out.println("Peer Id: " + Peer.peerId);

        Peer.start();
    }

    public Peer(String[] args) {

        initControlChannel(args[1], args[2]);
        //initDataChannel(args[3], args[4]);
        //initRecoveryChannel(args[5], args[6]);
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
        //new Thread(() -> handleDataChannel()).start();
        //new Thread(() -> handleDataRecoveryChannel()).start();
    }

    public void handleControlChannel() {

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                System.out.println("á espera...");

                mcSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("RECEBIDO: "+ new String(packet.getData(), 0, packet.getLength()));
        }
    }
}