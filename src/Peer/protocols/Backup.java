package Peer.protocols;

import Common.messages.PutChunkMessage;
import Common.messages.Version;
import Peer.Peer;
import Peer.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.logging.Logger;


public class Backup {
    private final int WINDOWSIZE = 10;
    private final Peer peer;
    private final int CHUNKSIZE;
    private final String CLIENT_DIRECTORY;

    public Backup(Peer peer, String clientDirectory, int chunksize) {
        this.peer = peer;
        CLIENT_DIRECTORY = clientDirectory;
        CHUNKSIZE = chunksize;
    }

    public void doBackup(File file, int replicationDegree) {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(CLIENT_DIRECTORY + file.getName()));
            int timeout = 200; // TODO this timeout is 200?
            int numberOfTries = 3;
            byte fileChunks[][] = Utils.getFileChunks(fileContent, CHUNKSIZE);
            String fileId = Utils.getEncodeHash(file.getName()+Long.toString(file.lastModified()));

            int index = 0;
            handle(fileChunks, new int[WINDOWSIZE], fileId, replicationDegree, timeout, numberOfTries, index);

            Logger.getGlobal().info("Finished backup...");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handle( byte[][] fileChunks, int[] chunks, String fileId, int replicationDegree,
                        int timeout, int numberOfTries, int index) throws IOException, InterruptedException {

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
    }
}
