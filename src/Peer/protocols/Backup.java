package Peer.protocols;

import Common.messages.PutChunkMessage;
import Peer.Peer;
import Peer.Utils;
import Peer.ChunkData;
import Peer.ChunkMetadata;
import Peer.FileData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;


public class Backup {
    private static int chunksPending = 0;

    private final int WINDOWSIZE = 10;
    private final int NUM_RETRIES = 5;          // number of times it resends the message
    private final int RESEND_TIMEOUT = 1000; // 1 second

    private final Peer peer;
    private final int CHUNKSIZE;
    private final String CLIENT_DIRECTORY;

    public Backup(Peer peer, String clientDirectory, int chunkSize) {
        this.peer = peer;
        CLIENT_DIRECTORY = clientDirectory;
        CHUNKSIZE = chunkSize;
    }

    public void doBackup(File file, int replicationDegree) {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(CLIENT_DIRECTORY + file.getName()));
            byte fileChunks[][] = Utils.getFileChunks(fileContent, CHUNKSIZE);
            Queue<ChunkData> chunkQueue = Utils.getQueueFromByteArray(fileChunks);
            String fileId = Utils.getEncodeHash(file.getName()+Long.toString(file.lastModified()));

            FileData fileData = new FileData(fileId, replicationDegree, chunkQueue);
            new Thread(() -> {
                try {
                    handle(fileData);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            Logger.getGlobal().info("Finished backup...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handle(FileData fileData) throws InterruptedException, IOException {
        while (fileData.getChunkData().size() > 0) {
            if (chunksPending >= WINDOWSIZE) {
                    sleep(50);
                    continue;
            }

            ChunkData chunkData = fileData.getChunkData().remove();
            PutChunkMessage message =  new PutChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), fileData.getFileId(),
                    chunkData.getChunkNo(), fileData.getRepDeg(), chunkData.getChunkData());
            chunksPending++;
            new Thread(() -> sendChunk(message, 0, RESEND_TIMEOUT)).start();
        }
    }

    private void sendChunk(PutChunkMessage message, int count, int timeout) {
         try {
            peer.MessageUtils.sendMessage(message);
            Utils.scheduleAction(() -> {
                ChunkMetadata metadata = peer.getChunkCount().get(message.getChunkUID());
                if (metadata != null && metadata.getRepDeg() >= message.getReplicationDeg()) {
                    // If it received all confirmations (at least the same as the replication degree)
                    chunksPending--;
                } else if (count >= NUM_RETRIES - 1) {
                    // If is last retry don't retry
                    Logger.getGlobal().warning("Failed to receive chunk stored confirmation: " +
                            message.getFileId() + " - " + message.getChunkNo());
                } else sendChunk(message, count + 1, timeout * 2); // Otherwise try again with double timeout
            }, timeout);
        } catch (IOException e) {
            Logger.getGlobal().severe("Failed to send chunk: " + e.getLocalizedMessage());
        }
    }



    /*private void handle( byte[][] fileChunks, int[] chunks, String fileId, int replicationDegree,
                        int timeout, int numberOfTries, int index) throws IOException {

        if(index >= fileChunks.length)
            return;

        int i = 0;
        while(i < WINDOWSIZE && index < fileChunks.length) {
            PutChunkMessage message = new PutChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), fileId, index, replicationDegree, fileChunks[index]);
            peer.MessageUtils.sendMessage(message);
            chunks[i] = index;
            i++;
            index++;
        }

        //resend chunks if failure
        checkChunksStored(chunks, fileId, fileChunks, replicationDegree, timeout, 5);

        handle(fileChunks, new int[WINDOWSIZE], fileId, replicationDegree, timeout, numberOfTries, index);

    }

    private void checkChunksStored(int[] chunks, String fileId, byte[][] fileChunks, int replicationDegree, int timeout, int nTries)  {

        if(nTries == 0) {
            Logger.getGlobal().info("Reached max number of tries...");
            return;
        }

        Utils.scheduleAction(() -> {
            boolean resend = false;
            synchronized (peer.getAcks()) {
                HashSet<Integer> set = peer.getAcks().get(fileId);
                Logger.getGlobal().info("Verify if all chunks are stored...");

                for (int i = 0; i < chunks.length; i++) {
                    if (!set.contains(chunks[i])) {
                        PutChunkMessage message = new PutChunkMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), fileId, chunks[i], replicationDegree, fileChunks[i]);
                        try {
                            peer.MessageUtils.sendMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Logger.getGlobal().info("Resending...");
                        resend = true;
                    }
                }
            }

            if(resend){
                checkChunksStored(chunks, fileId, fileChunks, replicationDegree, timeout*2, nTries-1);
                Logger.getGlobal().info("Resend chunks...");
            }
        }, timeout);
    }*/
}
