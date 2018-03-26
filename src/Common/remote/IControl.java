package Common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IControl extends Remote {
    String backup (byte[] fileContent, String fileName, String lastModification, int replicationDegree) throws RemoteException;
    String delete () throws RemoteException;
    String restore () throws RemoteException;
    String reclaim () throws RemoteException;
}
