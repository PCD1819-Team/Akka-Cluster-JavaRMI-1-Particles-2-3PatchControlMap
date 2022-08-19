package it.unibo.isi.pcd.remote;

import java.rmi.Remote;

import it.unibo.isi.pcd.utils.GeographicArea;

public interface RemotePatchInterface extends Remote {

	void setInEarlyAlert(final RemoteGuardianInterface guard) throws java.rmi.RemoteException;

	public void restorePatch() throws java.rmi.RemoteException;

	int getPatchID() throws java.rmi.RemoteException;

	GeographicArea getPatchArea() throws java.rmi.RemoteException;

}
