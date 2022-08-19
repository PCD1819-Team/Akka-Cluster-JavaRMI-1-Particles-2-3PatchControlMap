package it.unibo.isi.pcd.controller;

import java.rmi.registry.Registry;

import it.unibo.isi.pcd.remote.Dashboard;
import it.unibo.isi.pcd.remote.RemoteDashBoardInterface;
import it.unibo.isi.pcd.utils.RemoteObjectFactory;

public class MainController {

	public static void main(final String args[]) {

		try {
			final RemoteObjectFactory factory = RemoteObjectFactory.getInstance();

			final Registry registry = factory.createRegistry();

			final Dashboard localDashBoardReference = Dashboard.getInstance(registry);
			factory.exportRemoteDashboardObject(localDashBoardReference, registry);
			final RemoteDashBoardInterface remoteDashBoardReference = factory.getRemoteDashboardObject(registry);

			System.err.println("Ready");
		} catch (final Exception e) {
			System.err.println("Exception: " + e.toString());
			e.printStackTrace();
		}

	}

}
