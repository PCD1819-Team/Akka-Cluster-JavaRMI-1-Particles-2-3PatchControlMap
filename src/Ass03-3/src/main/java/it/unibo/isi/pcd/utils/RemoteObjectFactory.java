package it.unibo.isi.pcd.utils;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import it.unibo.isi.pcd.remote.Dashboard;
import it.unibo.isi.pcd.remote.Guardian;
import it.unibo.isi.pcd.remote.Patch;
import it.unibo.isi.pcd.remote.RemoteDashBoardInterface;
import it.unibo.isi.pcd.remote.RemoteGuardianInterface;
import it.unibo.isi.pcd.remote.RemotePatchInterface;

public class RemoteObjectFactory {
	private static final int DEFAULT_REGISTRY_PORT = 1099;
	private static final String REMOTE_REGISTY_ADDRESS = "127.0.0.1";
	private static final String REMOTE_DASHBOARD_ADDRESS = "rlmi:/it.unibo.isi.pcd.remote/Dashboard";
	private static final String REMOTE_PATCH_ADDRESS = "rlmi:/it.unibo.isi.pcd.remote/Patch";
	private static final String REMOTE_GUARDIAN_ADDRESS = "rlmi:/it.unibo.isi.pcd.remote/Guardian";
	private static final String ADRESS_NUMBER_REGEX = "_-_";

	private boolean createdRegistry;
	private static RemoteObjectFactory instance;

	private RemoteObjectFactory() {
		this.createdRegistry = false;
	}

	public synchronized static RemoteObjectFactory getInstance() {
		if (RemoteObjectFactory.instance == null) {
			RemoteObjectFactory.instance = new RemoteObjectFactory();
		}
		return RemoteObjectFactory.instance;
	}

	public synchronized void exportRemoteDashboardObject(final Dashboard localReference, final Registry registry)
			throws RemoteException, NotBoundException, AlreadyBoundException {
		final RemoteDashBoardInterface stub = (RemoteDashBoardInterface) UnicastRemoteObject
				.exportObject(localReference, 0);
		registry.bind(RemoteObjectFactory.REMOTE_DASHBOARD_ADDRESS, stub);
	}

	public synchronized RemoteDashBoardInterface getRemoteDashboardObject(final Registry registry)
			throws AccessException, RemoteException, NotBoundException {
		return (RemoteDashBoardInterface) registry.lookup(RemoteObjectFactory.REMOTE_DASHBOARD_ADDRESS);
	}

	public synchronized Registry createRegistry() throws RemoteException {
		if (!this.createdRegistry) {
			this.createdRegistry = true;
			return LocateRegistry.createRegistry(RemoteObjectFactory.DEFAULT_REGISTRY_PORT);
		} else {
			return null;
		}

	}

	public synchronized Registry getRegistry() throws RemoteException {
		if (this.createdRegistry) {
			final String registryHostAddress = RemoteObjectFactory.REMOTE_REGISTY_ADDRESS;
			return LocateRegistry.getRegistry(registryHostAddress);
		} else {
			return null;
		}
	}

	public synchronized void exportRemotePatchObject(final Patch localReference, final Registry registry)
			throws RemoteException, NotBoundException, AlreadyBoundException {

		final RemotePatchInterface stub = (RemotePatchInterface) UnicastRemoteObject.exportObject(localReference, 0);
		registry.bind(RemoteObjectFactory.REMOTE_PATCH_ADDRESS + localReference.getPatchID(), stub);
	}

	public synchronized RemotePatchInterface getRemotePatchObject(final int patchID, final Registry registry)
			throws AccessException, RemoteException, NotBoundException {
		return (RemotePatchInterface) registry.lookup(RemoteObjectFactory.REMOTE_PATCH_ADDRESS + patchID);
	}

	public synchronized void exportRemoteGuardianObject(final Guardian localReference, final Registry registry)
			throws RemoteException, NotBoundException, AlreadyBoundException {

		final RemoteGuardianInterface stub = (RemoteGuardianInterface) UnicastRemoteObject.exportObject(localReference,
				0);
		registry.bind(RemoteObjectFactory.REMOTE_GUARDIAN_ADDRESS + localReference.getGuardID()
				+ RemoteObjectFactory.ADRESS_NUMBER_REGEX + localReference.getPatchID(), stub);
	}

	public synchronized List<RemoteGuardianInterface> getRemoteGuardianObject(final int patchID,
			final Registry registry) throws AccessException, RemoteException, NotBoundException {
		final List<String> addresses = Arrays.asList(registry.list());
		final List<RemoteGuardianInterface> guardians = new ArrayList<RemoteGuardianInterface>();

		final List<String> guardiansAddresses = addresses.stream().filter(address -> {
			return address.contains(RemoteObjectFactory.REMOTE_GUARDIAN_ADDRESS);
		}).filter(guardianAddress -> {

			final String substring = guardianAddress.split(RemoteObjectFactory.ADRESS_NUMBER_REGEX)[1];
			return substring.equals(String.valueOf(patchID));
		}).collect(Collectors.toList());

		for (final String guardianAddress : guardiansAddresses) {
			guardians.add((RemoteGuardianInterface) registry.lookup(guardianAddress));
		}

		return guardians;
	}

}
