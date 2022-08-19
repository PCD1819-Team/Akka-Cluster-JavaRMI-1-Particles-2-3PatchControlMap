package it.unibo.isi.pcd.remote;

import java.rmi.Remote;

public interface RemoteGuardianInterface extends Remote {

	public AlertState getState() throws java.rmi.RemoteException;

	public void goInAlert() throws java.rmi.RemoteException;

	public void sendValue(final double newValue) throws java.rmi.RemoteException;

	public void restoreState() throws java.rmi.RemoteException;

}
