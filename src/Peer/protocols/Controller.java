package Peer.protocols;

import Common.remote.IControl;
import Peer.Peer;

import java.io.File;
import java.rmi.RemoteException;

public class Controller implements IControl {
    private final Peer peer;
    private final String CLIENT_DIRECTORY;
    private final int CHUNKSIZE;
    private final Backup backup;
    private final Restore restore;
    private final Delete delete;
    private final Reclaim reclaim;
    private final State state;
    private final ValidateDeleted validateDeleted;

    public Controller(Peer peer, String clientDirectory, int chunksize) {
        this.peer = peer;
        this.CLIENT_DIRECTORY = clientDirectory;
        this.CHUNKSIZE = chunksize;

        backup = new Backup(peer, CLIENT_DIRECTORY, CHUNKSIZE);
        restore = new Restore(peer, CLIENT_DIRECTORY, CHUNKSIZE);
        delete = new Delete(peer);
        reclaim = new Reclaim(peer);
        state = new State(peer);
        validateDeleted = new ValidateDeleted(peer);
    }

    /**
     * Receive file, split it in chunks and send them to the other peers
     * @param file
     * @param replicationDegree
     * @return name of operation //todo(?)
     */
    @Override
    public String backup(File file, int replicationDegree) {
        peer.isInitiatorPeer = true;
        backup.doBackup(file, replicationDegree);
        return "Operation backup...";
    }

    @Override
    public String delete(File file) {
        peer.isInitiatorPeer = true;
        delete.doDelete(file);
        return "Operation delete...";
    }

    @Override
    public String restore(File file) throws RemoteException {
        peer.isInitiatorPeer = true;
        restore.doRestore(file);
        return "Operation restore...";
    }

    @Override
    public String reclaim() throws RemoteException {
        reclaim.doReclaim();
        return "Operation reclaim...";
    }

    @Override
    public String state() throws RemoteException {
        return state.getState();
    }

    public void validateDeleted() {
        validateDeleted.doValidateDeleted();
    }
}
