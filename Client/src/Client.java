import java.io.IOException;
import java.net.*;

public class Client {

    private String mcast_addr;
    private int mcast_port;

    public Client(String[] args) {
        mcast_addr = args[0];
        mcast_port = Integer.parseInt(args[1]);
    }

    public static void main(String[] args) throws UnknownHostException {

        if (args.length != 2) {
            System.out.println("Usage: java Client <mcast_addr> <mcast_port>");
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