package Common.remote;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IControl extends Remote {
    String backup (File file, int replicationDegree) throws RemoteException;
    String delete (File file) throws RemoteException;
    String restore (File file) throws RemoteException;
    String reclaim () throws RemoteException;
}
