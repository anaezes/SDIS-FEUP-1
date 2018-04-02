package Peer.protocols;

import Common.messages.GetDeletedMessage;
import Peer.Peer;
import Peer.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class ValidateDeleted {
    private final int RESEND_DELAY = 1000;
    private final int NUM_TRIES = 3;
    private Peer peer;

    public ValidateDeleted(Peer peer) {
        this.peer = peer;
    }

    public void doValidateDeleted() {
        Logger.getGlobal().info("Checking if any deleted message was send while offline...");
        File[] files = new File(peer.getFileSystemPath()).listFiles();
        for(File file: files) {
            if (file.isDirectory()) {
                new Thread(() -> handleValidation(file, RESEND_DELAY, 0)).start();
            }
        }
    }

    private void handleValidation(File file, int delay, int count) {
        if (count >= NUM_TRIES) return;
        GetDeletedMessage message = new GetDeletedMessage(Peer.PROTOCOL_VERSION, peer.getPeerId(), file.getName());
        try {
            peer.MessageUtils.sendMessage(message);
        } catch (IOException e) {
            Logger.getGlobal().warning("Couldn't send GetDeleted message: " + e.getLocalizedMessage());
        }
        Utils.scheduleAction(() -> {
            if (!peer.getDeletedFiles().contains(file.getName()))
                handleValidation(file, delay * 2, count + 1);
        }, delay);
    }
}
