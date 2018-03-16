package Client;

import Common.remote.IControl;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    String peerId;
    Registry registry;
    IControl control;

    private final String operation;
    String opnd_1;
    String opnd_2;

    //private Message message;

    public Client(String[] args) {

        peerId = args[0];
        operation = args[1];

        // make RMI connection with Peer
        try {
            registry = LocateRegistry.getRegistry(null);
            control = (IControl) registry.lookup("peer" + peerId);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException, RemoteException {

        if (args.length < 1 || args.length > 5) {
            System.out.println("Usage: java Client <peer_ap> <operation> <opnd_1> <opnd_2>");
            return;
        }

        //create  client
        Client client = new Client(args);

       //make request
        try {
            makeRequest(client);
        }
        catch (RemoteException e){
            System.out.println("Error: " + e.toString());
        }

    }

    private static void makeRequest(Client client) throws RemoteException {

        String response;

        switch (client.getOperation()) {
            case "BACKUP":
                response = client.control.backup();
                break;
            case "DELETE":
                response = client.control.delete();
                break;
            case "RESTORE":
                response = client.control.restore();
                break;
            case "RECLAIM":
                response = client.control.reclaim();
                break;
            default:
                response = "Invalid Operation";
        }

        System.out.println("Response: " + response);
    }

    public String getOperation() {
        return this.operation;
    }
}