package Client;

import Common.remote.IControl;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class Client {
    private final String FILES_DIRECTORY = System.getProperty("user.dir") + File.separator + "filesystem" + File.separator + "client" + File.separator;

    public enum Commands {
        CMD_BACKUP("BACKUP"),
        CMD_RESTORE("RESTORE"),
        CMD_DELETE("DELETE"),
        CMD_RECLAIM("RECLAIM"),
        CMD_STATE("STATE");

        private final String text;

        /**
         * @param text
         */
        Commands(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private String peerId;
    private Registry registry;
    private IControl control;

    private String operation;
    private String fileName;
    private int replication;
    private String diskSpace;

    //private Message message;

    public Client(String[] args) {

        peerId = args[0];
        operation = args[1];

        if(operation.equals(Commands.CMD_BACKUP.toString())) {
            fileName = args[2];
            replication = Integer.parseInt(args[3]);
        }
        else if(operation.equals(Commands.CMD_DELETE.toString()) || operation.equals(Commands.CMD_RESTORE.toString())) {
            fileName = args[2];
        }
        else if(operation.equals(Commands.CMD_RECLAIM.toString())) {
            diskSpace = args[2];
        }

        // make RMI connection with Peer
        try {
            registry = LocateRegistry.getRegistry(null);
            control = (IControl) registry.lookup("peer" + peerId);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void makeRequest() throws IOException {

        String response;
        File file = new File(FILES_DIRECTORY+fileName);

        switch (getOperation()) {
            case "BACKUP":
                if(!file.exists())
                    throw new IOException();
                response = control.backup(file, replication);
                break;
            case "DELETE":
                if(!file.exists())
                    throw new IOException();
                response = control.delete(file);
                break;
            case "RESTORE":
                if(!file.exists())
                    throw new IOException();
                response = control.restore(file);
                break;
            case "RECLAIM":
                response = control.reclaim();
                break;
            case "STATE":
                response = control.state();
                break;
            default:
                response = "Invalid Operation";
        }

        System.out.println("Response: " + response);
    }

    public String getOperation() {
        return this.operation;
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
            client.makeRequest();
        }
        catch (IOException e){
            System.err.println("Error: " + e.toString());
        }
    }
}