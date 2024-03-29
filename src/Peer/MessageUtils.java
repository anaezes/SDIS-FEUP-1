package Peer;

import Common.messages.*;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class MessageUtils {
    private final Peer peer;
    private final int NUMBER_TRIES;
    private PutChunkMessage lastPutChunkReceived; // To handleDeleteMessage know if was received before sending

    public MessageUtils(Peer peer, int numberOfTries) {
        this.peer = peer;
        this.NUMBER_TRIES = numberOfTries;
    }

    public void handleGetChunkMessage(GetChunkMessage message) throws InterruptedException, IOException {
        // tenho ? -> obtenho o chunk se não aborta
        File file = new File(peer.getFileSystemPath() + File.separator + message.getFileId());
        if(!file.exists())
            return;

        File chunk = new File(peer.getFileSystemPath() + File.separator + message.getFileId() + File.separator + message.getChunkNo());
        if(!chunk.exists())
            return;

        Utils.scheduleAction(() -> {
            // verificar se entretanto já alguem enviou
            // se sim, aborta, se não envio
            if(peer.getChunksSent().containsKey(message.getFileId())) {
                if (peer.getChunksSent().get(message.getFileId()).contains(message.getChunkNo())) {
                    return;
                }
            }
            try {
                //send message STORED chunk
                ChunkMessage chunkMessage = new ChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), message.getFileId(),
                        message.getChunkNo(), Files.readAllBytes(chunk.toPath()));
                if (!sendThroughTCP(message, chunkMessage, false))
                    sendMessage(chunkMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, (long )(Math.random() * peer.DELAY_MS));
    }

    public void handleDeleteMessage(DeleteMessage message) {
        if(message.getSenderId() == peer.getPeerId())
            return;

        peer.addDeletedFile(message.getFileId());
        File file = new File(peer.getFileSystemPath() + File.separator + message.getFileId());

        if(!file.exists())
            return;

        if(!Utils.deleteFile(file))
            Logger.getGlobal().severe("Couldn't delete file: " + file.getAbsolutePath());
    }

    public void handleRemovedMessage(RemovedMessage message) throws IOException {
        ChunkMetadata metadata = peer.getChunkCount().get(message.getChunkUID());
        metadata.getPeerIds().remove(message.getSenderId());

        if (message.getSenderId() == peer.getPeerId()) {
            peer.ignorePutChunkUID(message.getChunkUID());
            return;
        }

        if (metadata.getPeerIds().size() < metadata.getRepDeg()) {
            lastPutChunkReceived = null;
            byte[] chunk = Utils.getChunkFromFilesystem(peer, metadata.getFileId(), metadata.getChunkNo());

            if (chunk == null) Logger.getGlobal().info("Chunk not found in peer filesystem, ignoring");
            else {
                Utils.scheduleAction(() -> {
                    try {
                        if (lastPutChunkReceived == null || lastPutChunkReceived.getChunkUID() != message.getChunkUID()) {
                            PutChunkMessage pcMessage = new PutChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(),
                                    metadata.getFileId(), metadata.getChunkNo(), metadata.getRepDeg(), chunk);

                            peer.MessageUtils.sendMessage(pcMessage); // If it hasn't received a PutChunk message to the same chunk

                        } else
                            Logger.getGlobal().info("Already received PutChunk for this chunk, aborting starting backup subprotocol");
                    } catch (IOException e) {
                        Logger.getGlobal().warning("Couldn't wait to send PutChunk message: " + e.getLocalizedMessage());
                    }
                }, peer.DELAY_MS);
            }
        }
    }

    /**
     * Creates root directory, if non-existent, and stores the received chunk
     * @param message
     */
    public void handlePutChunkMessage(PutChunkMessage message) throws IOException {
        lastPutChunkReceived = message;

        // Ignores if message comes from own peer
        if(message.getSenderId() == peer.getPeerId())
            return;

        // Ignores message if chunk is listed in the IgnorePutChunk array
        if (peer.getIgnorePutChunkUID().contains(message.getChunkUID())) {
            Logger.getGlobal().info("Chunk listed in the ignore list, ignoring message...");
            return;
        }

        Utils.scheduleAction(() -> {
            synchronized (peer.getChunkCount()) {
                if (peer.getChunkCount().containsKey(message.getChunkUID()) &&
                        peer.getChunkCount().get(message.getChunkUID()).getPeerIds().size() >= message.getReplicationDeg()) {
                    Logger.getGlobal().info("Replication degree reached, not storing chunk...");
                    return;
                }
            }

            try {
                StoredMessage storedMessage = new StoredMessage(message.getVersion(), peer.getPeerId(),
                        message.getFileId(), message.getChunkNo());
                //send message STORED chunk
                sendMessage(storedMessage);
            Path path = Paths.get(peer.getFileSystemPath() + "/" + message.getFileId());
                if (!Files.exists(path))
                    Files.createDirectory(path);
                Files.write(Paths.get(path.toString() + "/" + message.getChunkNo()), Utils.trim(message.getBody()));


            } catch (IOException e) {
                e.printStackTrace();
            }

            peer.logCapacityInfo();
            peer.validateStorageCapacity();
        }, (long )(Math.random() * peer.DELAY_MS));
    }

    /**
     * Sends message according to its type
     * @param message
     */
    public void sendMessage(Message message) throws IOException {
        Logger.getGlobal().info("Send message " + message.getMessageType());
        DatagramPacket packet;

        switch (message.getMessageType()) {
            case PUTCHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMdbAddr(), peer.getMdbPort());
                peer.getMdbSocket().send(packet);
                break;
            case STORED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcAddr(), peer.getMcPort());
                peer.getMcSocket().send(packet);
                break;
            case DELETE:
                for(int i = 0; i < NUMBER_TRIES; i++) {
                    packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcAddr(), peer.getMcPort());
                    peer.getMcSocket().send(packet);
                }
                break;
            case CHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMdrAddr(), peer.getMdrPort());
                peer.getMdrSocket().send(packet);
                break;
            case GETCHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcAddr(), peer.getMcPort());
                peer.getMcSocket().send(packet);
                break;
            case REMOVED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcAddr(), peer.getMcPort());
                peer.getMcSocket().send(packet);
                break;
            case GET_DELETED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcAddr(), peer.getMcPort());
                peer.getMcSocket().send(packet);
        }

    }

    private boolean sendThroughTCP(Message inMessage, ChunkMessage outMessage, boolean isRetry) {
        try {
            Socket socket = new Socket(InetAddress.getByName("localhost"), inMessage.getSenderId() * 10);
            if (socket.isConnected()) { // If can send data through TCP socket
                ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
                toServer.writeObject(outMessage);
                toServer.flush();
                toServer.close();
                socket.close();
                Logger.getGlobal().info("Chunk " + outMessage.getChunkNo() + "sent through TCP");
                return true;
            }
        } catch (IOException e) {
            Logger.getGlobal().warning("Couldn't send data through TCP: " + e.getLocalizedMessage());
        }

        if (!isRetry) {
            try {
                Thread.sleep(peer.DELAY_MS);
                return sendThroughTCP(inMessage, outMessage, true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Logger.getGlobal().warning("Couldn't find a TCP connection, falling back to multicast...");
        return false;
    }
}
