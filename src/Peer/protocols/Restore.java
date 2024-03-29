package Peer.protocols;

import Common.messages.ChunkMessage;
import Common.messages.GetChunkMessage;
import Common.messages.Message;
import Peer.Peer;
import Peer.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Restore {
    private final Peer peer;
    private final int CHUNKSIZE;
    private final String CLIENT_DIRECTORY;
    private Thread tcpThread;

    public Restore(Peer peer, String clientDirectory, int chunksize) {
        this.peer = peer;
        CLIENT_DIRECTORY = clientDirectory;
        CHUNKSIZE = chunksize;
    }

    public void doRestore(File file) {
        startTcpListener();
        try {
            byte fileContent[] = Files.readAllBytes(Paths.get(CLIENT_DIRECTORY + file.getName()));

            //  calcular quantos chunks serão necessários
            int noChunks = fileContent.length/CHUNKSIZE + 1;

            // pede sequencialmente e colecionar num hashmap<chunkNo, conteúdo>
            getAllChunksFile(file, noChunks);

            Utils.scheduleAction(() -> {
                String fileId = Utils.getEncodeHash(file.getName()+file.lastModified());
                if(!peer.getRestore().containsKey(fileId) || peer.getRestore().get(fileId).size() < noChunks) {
                        doRestore(file);
                }
                else{
                    //restore file
                    try {
                        restoreFile(file, fileContent.length, fileId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Logger.getGlobal().info("Restore file completed");
                }
            }, peer.DELAY_MS);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void restoreFile(File file, int size, String fileId) throws IOException {
        HashMap<String, HashMap<Integer, byte[]>> hash = peer.getRestore();
        HashMap<Integer, byte[]>  fileChunks = hash.get(fileId);
        Set entrySet = fileChunks.entrySet();
        Iterator it = entrySet.iterator();

        // Iterate through HashMap entries(Key-Value pairs)
        FileOutputStream fos = new FileOutputStream(CLIENT_DIRECTORY + File.separator +
                "restoredfiles" + File.separator + file.getName(), false);

        while(it.hasNext() && size > 0){
            Map.Entry content = (Map.Entry)it.next();
            byte[] bytes = Utils.trim((byte[]) content.getValue());
            fos.write(bytes, 0, Math.min(bytes.length, size));
            size -= bytes.length;
        }

        fos.close();
    }

    private void getAllChunksFile(File file, int noChunks) throws IOException, InterruptedException {

        String fileId = Utils.getEncodeHash(file.getName()+file.lastModified());

        for(int i = 0; i < noChunks; i ++){
            GetChunkMessage message = new GetChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), fileId, i);
            peer.MessageUtils.sendMessage(message);
        }
    }

    private void startTcpListener() {
        tcpThread = new Thread(() -> {
            try {
                Socket clientSocket = peer.getTcpSocket().accept();
                ObjectInputStream inMessage = new ObjectInputStream(clientSocket.getInputStream());
                ChunkMessage message = (ChunkMessage)inMessage.readObject();
                Logger.getGlobal().info("Received chunk " + message.getChunkNo() + " through TCP: " + message.getMessageType() + " from peer " + message.getSenderId());
                peer.CommunicationChannels.handleChunkMessage(message);
                inMessage.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        tcpThread.start();

    }
}
