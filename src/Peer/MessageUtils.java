package Peer;

import Common.messages.*;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Thread.sleep;

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

        // calcular tempo de espera (random entre 0 e 400ms)
        sleep((long )(Math.random() * 400));

        // verificar se entretanto já alguem enviou
        // se sim, aborta, se não envio
        if(peer.getChunksSent().containsValue(message.getFileId()))
            if(peer.getChunksSent().get(message.getFileId()).contains(message.getChunkNo()))
                return;

        //send message STORED chunk
        ChunkMessage chunkMessage = new ChunkMessage(new Version(1, 0), peer.getPeerId(), message.getFileId(),
                message.getChunkNo(), Files.readAllBytes(chunk.toPath()));
        sendMessage(chunkMessage);

    }

    public void handleDeleteMessage(DeleteMessage message) throws IOException {
        if(message.getSenderId() == peer.getPeerId())
            return;

        File file = new File(peer.getFileSystemPath() + File.separator + message.getFileId());

        if(!file.exists())
            return;

        if(!Utils.deleteFile(file))
            System.out.println("Error to delete a file! ");
    }

    /**
     * Creates root directory, if non-existent, and stores the received chunk
     * @param message
     */
    public void handlePutChunkMessage(PutChunkMessage message) throws IOException {

        if(message.getSenderId() == peer.getPeerId())
            return;

        Path path = Paths.get(peer.getFileSystemPath() + "/" + message.getFileId());
        if (!Files.exists(path))
            Files.createDirectory(path);
        Files.write(Paths.get(path.toString() + "/" + message.getChunkNo()), message.getBody());

        //send message STORED chunk
        StoredMessage storedMessage = new StoredMessage(message.getVersion(), peer.getPeerId(), message.getFileId(),
                message.getChunkNo());
        sendMessage(storedMessage);
    }

    /**
     * Sends message according to its type
     * @param message
     */
    public void sendMessage(Message message) throws IOException {
        System.err.println("\nSend message " + message.getMessageType() + "\n");
        DatagramPacket packet;

        switch (message.getMessageType()) {
            case PUTCHUNK:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMdbSocket().getLocalSocketAddress());
                peer.getMdbSocket().send(packet);
                break;
            case STORED:
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcSocket().getLocalSocketAddress());
                peer.getMcSocket().send(packet);
                break;
            case DELETE:
                for(int i = 0; i < NUMBER_TRIES; i++) {
                    packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcSocket().getLocalSocketAddress());
                    peer.getMcSocket().send(packet);
                }
                break;
            case CHUNK:
                System.out.println("VOU MANDAR CHUNK");
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMdrSocket().getLocalSocketAddress());
                peer.getMdrSocket().send(packet);
                break;
            case GETCHUNK:
                System.out.println("VOU PEDIR CHUNK");
                packet = new DatagramPacket(message.getBytes(), message.getBytes().length, peer.getMcSocket().getLocalSocketAddress());
                peer.getMcSocket().send(packet);
                break;
        }

    }
}
