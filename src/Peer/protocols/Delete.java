package Peer.protocols;

import Common.messages.DeleteMessage;
import Common.messages.Version;
import Peer.Peer;
import Peer.Utils;

import java.io.File;
import java.io.IOException;

public class Delete {
    private final Peer peer;

    public Delete(Peer peer) {
        this.peer = peer;
    }

    public void doDelete(File file) {
        try {
            String fileId = Utils.getEncodeHash(file.getName()+file.lastModified());
            DeleteMessage message = new DeleteMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), fileId);
            peer.MessageUtils.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
