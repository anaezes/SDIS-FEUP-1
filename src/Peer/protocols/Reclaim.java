package Peer.protocols;

import Common.messages.RemovedMessage;
import Common.messages.Version;
import Peer.Peer;
import Peer.ChunkMetadata;

import java.io.IOException;
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
            RemovedMessage message = new RemovedMessage(new Version(1, 0), peer.getPeerId(), metadata.getFileId(), metadata.getChunkNo());
            try {
                peer.MessageUtils.sendMessage(message);
            } catch (IOException e) {
                Logger.getGlobal().warning("Error sending reclaim message: " + e.getLocalizedMessage());
            }
        } else {
            Logger.getGlobal().warning("Cannot reclaim space: no key to remove!");
        }
    }
}
