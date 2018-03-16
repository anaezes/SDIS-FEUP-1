package Client;

import Common.remote.IControl;

import java.io.IOException;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private String mcast_addr;
    private int mcast_port;
    //private Message message;

    public Client(String[] args) {
        mcast_addr = args[0];
        mcast_port = Integer.parseInt(args[1]);

        try {
            Registry registry = LocateRegistry.getRegistry(null);
            IControl control = (IControl) registry.lookup("Hello");
            String response = control.Backup();
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException {

        if (args.length > 1 || args.length < 5) {
            System.out.println("Usage: java Client <peer_ap> <operation> <opnd_1> <opnd_2>");
            return;
        }

        //create  client
        Client client = new Client(args);

        //Do request to peer
        client.sendMessage(args, client);
    }

    private void sendMessage(String[] args, Client client) {
        try {
            // send request
            DatagramSocket socket = new DatagramSocket();

            String request = "esta é a string de teste que vai ser enviada do cliente para o servidor :)";
            System.out.println(request);

            byte[] bfr = request.getBytes();
            DatagramPacket packet = new DatagramPacket(bfr, bfr.length, InetAddress.getByName(client.mcast_addr), client.mcast_port);
            socket.send(packet);

            socket.close();

        } catch(SocketException e){
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}