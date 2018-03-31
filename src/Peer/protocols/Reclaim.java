package Peer.protocols;

import Common.messages.RemovedMessage;
import Common.messages.Version;
import Peer.Peer;
import Peer.ChunkMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Reclaim {
    private final Peer peer;

    public Reclaim(Peer peer) {
        this.peer = peer;
    }

    public void doReclaim() {
        int minRepDeg = Integer.MAX_VALUE;
        String keyToRemove = "";

        for (String key : peer.getChunkCount().keySet()) {
            if (peer.getChunkCount().get(key).getRepDeg() < minRepDeg) {
                minRepDeg = peer.getChunkCount().get(key).getRepDeg();
                keyToRemove = key;
            }
        }

        if (keyToRemove.length() > 0) {
            ChunkMetadata metadata = peer.getChunkCount().get(keyToRemove);
            RemovedMessage message = new RemovedMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), metadata.getFileId(), metadata.getChunkNo());
            removeChunkFromDisk(metadata);
            peer.logCapacityInfo();

            try {
                peer.MessageUtils.sendMessage(message);
            } catch (IOException e) {
                Logger.getGlobal().warning("Error sending reclaim message: " + e.getLocalizedMessage());
            }
        } else {
            Logger.getGlobal().warning("Cannot reclaim space: no key to remove!");
        }
    }

    private boolean removeChunkFromDisk(ChunkMetadata metadata) {
        File chunk = Paths.get(peer.getFileSystemPath(), metadata.getFileId(), Integer.toString(metadata.getChunkNo())).toFile();
        if (chunk.exists()){
            boolean deleted = chunk.delete();
            if (deleted) Logger.getGlobal().info("Chunk removed: " + chunk.getAbsolutePath());
            else Logger.getGlobal().warning("Chunk couldn't be removed: " + chunk.getAbsolutePath());
        }
        else {
            Logger.getGlobal().warning("Chunk not found: " + chunk.getAbsolutePath());
            return false;
        }

        File chunkFolder = chunk.getParentFile();
        if (chunkFolder.list().length == 0) chunkFolder.delete();
        return true;
    }
}
