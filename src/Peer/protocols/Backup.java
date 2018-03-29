package Peer.protocols;

import Common.messages.PutChunkMessage;
import Common.messages.Version;
import Peer.Peer;
import Peer.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.Thread.sleep;

public class Backup {
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
            handle(fileContent, file.getName(), Long.toString(file.lastModified()), replicationDegree, timeout, numberOfTries);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handle(byte[] fileContent, String fileName, String lastModification, int replicationDegree,
                        int timeout, int numberOfTries) throws IOException, InterruptedException {

        if(numberOfTries == 0)
            return;

        byte fileChunks[][] = Utils.getFileChunks(fileContent, CHUNKSIZE);
        int chunks[] = new int[fileChunks.length];
        int i = 0;
        String fileId = Utils.getEncodeHash(fileName+lastModification);

        while(i < fileChunks.length) {
            PutChunkMessage message = new PutChunkMessage(new Version(1, 0), peer.getPeerId(), fileId, i, replicationDegree, fileChunks[i]);
            peer.MessageUtils.sendMessage(message);
            chunks[i] = i;
            i++;
        }

        sleep(timeout);

        timeout = timeout*2;
        numberOfTries--;

        //resend chunks if failure
        if(chunks.length < replicationDegree) {
            handle(fileContent, fileName, lastModification, replicationDegree, timeout, numberOfTries);
        }
    }
}
