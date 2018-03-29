package Peer;

import Common.remote.IControl;
import Peer.protocols.Controller;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Peer {
    private static final int NUMBER_TRIES = 3;
    private final String FILES_DIRECTORY = System.getProperty("user.dir") + File.separator +"filesystem" + File.separator
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

    //store the restore chunks
    private final HashMap<String, HashMap<Integer, byte[]>> restore = new HashMap<>();

    //store the flags of chunks sent
    private final HashMap<String, HashSet<Integer>> chunksSent = new HashMap<>();

    // Contains communication channel handlers
    public final CommunicationChannels CommunicationChannels;

    // Contains handlers to process messages
    public final MessageUtils MessageUtils;

    // Contains handlers to all implemented protocols
    public final Controller ProtocolController;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Peer <Peer_id> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>");
            return;
        }
        Logger.getGlobal().setLevel(Level.ALL);

        try {
            Peer peer = new Peer(args);
            Logger.getGlobal().info("Peer created with ID: " + peer.getPeerId());
            peer.start();
        } catch (IOException e) {
            Logger.getGlobal().severe("Cannot create peer, aborting!");
            Logger.getGlobal().severe(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
    * Constructor of Peer
    */
    public Peer(String[] args) throws IOException {
        Logger.getGlobal().info("Creating Peer...");
        this.peerId = Integer.parseInt(args[0]);

        // Verify if this peer base directory exists. If not creates it.
        initFilesystem();

        // Creates communication channel handlers
        this.CommunicationChannels = new CommunicationChannels(this, CHUNKSIZE);

        // Creates message handlers
        this.MessageUtils = new MessageUtils(this, NUMBER_TRIES);

        // Creates new protocol controller
        this.ProtocolController = new Controller(this, CLIENT_DIRECTORY, CHUNKSIZE);

        // Initiates communication channels
        initControlChannel(args[1], args[2]);
        initDataChannel(args[3], args[4]);
        initRecoveryChannel(args[5], args[6]);
        initRMIChannel(1099);
    }

    /**
    * Init channel for protocols messages
    * @param address
    * @param port
    */
    private void initControlChannel(String address, String port) {
        Logger.getGlobal().info("Initializing protocols channel at " + address + ":" + port + "...");
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
        Logger.getGlobal().info("Initializing data channel at " + address + ":" + port + "...");
        try {
            mdbAddr = InetAddress.getByName(address);
            mdbPort = Integer.parseInt(port);
            Logger.getGlobal().info("Initializing data channel at " + mdbAddr + ":" + mdbPort + "...");
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
        Logger.getGlobal().info("Initializing recovery channel at " + address + ":" + port + "...");
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

    /**
     * Init RMI channel for communication with client
     * @param port
     * @throws RemoteException
     */
    private void initRMIChannel(int port) throws RemoteException {
        Logger.getGlobal().info("Initializing RMI channel at port " + port + "...");
        Registry registry;

        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (ExportException e) {
            if (e.toString().contains("Port already in use")) {
                registry = LocateRegistry.getRegistry();
            } else {
                throw e;
            }
        }

        IControl control = (IControl) UnicastRemoteObject.exportObject(ProtocolController, 0);
        try {
            // Bind the remote object's stub in the registry
            registry.bind( "peer" + this.getPeerId(), control);
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if peer directory exist, if non-existent create.
     */
    public void initFilesystem() throws IOException {
        Logger.getGlobal().info("Initializing peer filesystem:\n" + this.getFileSystemPath());
        Path path = Paths.get(this.getFileSystemPath());
        if (!Files.exists(path))
            Files.createDirectory(path);
    }


    /**
    * Creates threads that wait for messages from channels
    */
    public void start() {
        Logger.getGlobal().info("Starting control channel");
        CommunicationChannels.getControlChannelThread().start();

        Logger.getGlobal().info("Starting data channel");
        CommunicationChannels.getDataChannelThread().start();

        Logger.getGlobal().info("Starting data recovery channel");;
        CommunicationChannels.getRecoveryChannelThread().start();
    }

    /**
     * Getters for Peer variables
     */
    public HashMap<String, HashSet<Integer>> getAcks() {
        return acks;
    }

    public MulticastSocket getMcSocket() {
        return mcSocket;
    }

    public MulticastSocket getMdbSocket() {
        return mdbSocket;
    }

    public MulticastSocket getMdrSocket() {
        return mdrSocket;
    }

    public HashMap<String, HashSet<Integer>> getChunksSent() {
        return chunksSent;
    }

    public HashMap<String, HashMap<Integer, byte[]>> getRestore() {
        return restore;
    }

    public InetAddress getMcAddr() {
        return mcAddr;
    }

    public int getMcPort() {
        return mcPort;
    }

    public InetAddress getMdbAddr() {
        return mdbAddr;
    }

    public int getMdbPort() {
        return mdbPort;
    }

    public InetAddress getMdrAddr() {
        return mdrAddr;
    }

    public int getMdrPort() {
        return mdrPort;
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

}