package Peer;

import Common.messages.*;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.logging.Logger;

public class MessageUtils {
    private final Peer peer;
    private final int NUMBER_TRIES;

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
                ChunkMessage chunkMessage = new ChunkMessage(new Version(1, 0), peer.getPeerId(), message.getFileId(),
                        message.getChunkNo(), Files.readAllBytes(chunk.toPath()));
                sendMessage(chunkMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, (long )(Math.random() * 400));
    }

    public void handleDeleteMessage(DeleteMessage message) throws IOException {
        if(message.getSenderId() == peer.getPeerId())
            return;

        File file = new File(peer.getFileSystemPath() + File.separator + message.getFileId());

        if(!file.exists())
            return;

        if(!Utils.deleteFile(file))
            Logger.getGlobal().severe("Couldn't delete file: " + file.getAbsolutePath());
    }

    /**
     * Creates root directory, if non-existent, and stores the received chunk
     * @param message
     */
    public void handlePutChunkMessage(PutChunkMessage message) throws IOException {

        if(message.getSenderId() == peer.getPeerId())
            return;

        Utils.scheduleAction(() -> {
            synchronized (peer.getChunkCount()) {
                String hash = message.getFileId() + message.getChunkNo();

                if (peer.getChunkCount().containsKey(hash)) {
                    if (peer.getChunkCount().get(hash).size() >= message.getReplicationDeg()) {
                        Logger.getGlobal().info("Replication degree reached, not storing chunk...");
                        return;
                    }
                }
            }
            Path path = Paths.get(peer.getFileSystemPath() + "/" + message.getFileId());
            try {
                if (!Files.exists(path))
                    Files.createDirectory(path);
                Files.write(Paths.get(path.toString() + "/" + message.getChunkNo()), Utils.trim(message.getBody()));

                //send message STORED chunk
                StoredMessage storedMessage = new StoredMessage(message.getVersion(), peer.getPeerId(), message.getFileId(),
                        message.getChunkNo());
                sendMessage(storedMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, (long )(Math.random() * 400));

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
        }

    }
}
