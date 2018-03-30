package Peer;

import Common.messages.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class CommunicationChannels {
    private final Peer peer;
    private final int CHUNKSIZE;
    private final Thread controlChannelThread;
    private final Thread dataChannelThread;
    private final Thread recoveryChannelThread;

    /**
     * Constructor for communication channels
     * @param peer reference to peer
     * @param chunkSize the max size of each chunk
     */
    public CommunicationChannels(Peer peer, int chunkSize) {
        this.peer = peer;
        this.CHUNKSIZE = chunkSize;

        controlChannelThread = new Thread(() -> {
            handleControlChannel();
        });

        dataChannelThread = new Thread(() -> {
            handleDataChannel();
        });

        recoveryChannelThread = new Thread(() -> {
            handleRecoveryChannel();
        });
    }

    public Thread getControlChannelThread() {
        return controlChannelThread;
    }

    public Thread getDataChannelThread() {
        return dataChannelThread;
    }

    public Thread getRecoveryChannelThread() {
        return recoveryChannelThread;
    }

    /**
     * Handle protocols channel messages
     */
    private void handleControlChannel() {
        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                peer.getMcSocket().receive(packet);
                Message message = Message.parseMessage(packet);
                Logger.getGlobal().info("Received message on MC Channel: " + message.getMessageType());

                if(message.getSenderId() == peer.getPeerId())
                    continue;

                if(message instanceof StoredMessage) {
                    //store sent ack
                    HashSet<Integer> set = peer.getAcks().getOrDefault(message.getFileId(), new HashSet<>());
                    set.add(message.getChunkNo());
                    peer.getAcks().putIfAbsent(message.getFileId(), set);
                }
                else if(message instanceof DeleteMessage){
                    peer.MessageUtils.handleDeleteMessage((DeleteMessage) message);
                }
                else if(message instanceof GetChunkMessage){
                peer.MessageUtils.handleGetChunkMessage((GetChunkMessage) message);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle data channel messages
     */
    private void handleDataChannel() {
        byte[] buffer = new byte[CHUNKSIZE+256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                peer.getMdbSocket().receive(packet);

                Message message = Message.parseMessage(packet);
                Logger.getGlobal().info("Received message on MDB Channel: " + message.getMessageType());

                if(message instanceof PutChunkMessage)
                    peer.MessageUtils.handlePutChunkMessage((PutChunkMessage) message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle data recovery channel messages
     */
    private void handleRecoveryChannel() {
        byte[] buffer = new byte[CHUNKSIZE+256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                peer.getMdrSocket().receive(packet);

                Message message = Message.parseMessage(packet);

                Logger.getGlobal().info("Received message on MDR Channel: " + message.getMessageType());

                //outros peers
                if(message instanceof ChunkMessage) {

                    //other peer
                    HashSet<Integer> set = peer.getChunksSent().getOrDefault(message.getFileId(), new HashSet<>());
                    set.add(message.getChunkNo());
                    peer.getChunksSent().putIfAbsent(message.getFileId(), set);

                    //store chunk content - main peer
                    HashMap<Integer, byte[]> chunk = peer.getRestore().getOrDefault(message.getFileId(), new HashMap<>());
                    chunk.put(message.getChunkNo(), message.getBody());
                    peer.getRestore().putIfAbsent(message.getFileId(), chunk);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
