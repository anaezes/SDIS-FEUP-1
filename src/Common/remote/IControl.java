package Common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IControl extends Remote {
    String Backup () throws RemoteException;
}
