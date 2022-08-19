package it.unibo.isi.pcd.remote;

import java.awt.geom.Point2D;
import java.rmi.Remote;

import it.unibo.isi.pcd.utils.GeographicArea;
import it.unibo.isi.pcd.utils.OutOfMapException;

public interface RemoteDashBoardInterface extends Remote {

	public void notifyAlertPatch(final int patchID) throws java.rmi.RemoteException;

	public void notifyEarlyAlertGuardian(final int patchID) throws java.rmi.RemoteException;

	public void notifyUpdateSensorPosition(final int sensorID, final Point2D position) throws java.rmi.RemoteException;

	public GeographicArea getMapArea() throws java.rmi.RemoteException;

	public int getRelatedPatchID(Point2D position) throws OutOfMapException, java.rmi.RemoteException;
}