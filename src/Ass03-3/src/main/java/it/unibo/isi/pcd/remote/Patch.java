package it.unibo.isi.pcd.remote;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

import it.unibo.isi.pcd.utils.GeographicArea;
import it.unibo.isi.pcd.utils.RemoteObjectFactory;

public class Patch implements RemotePatchInterface, Serializable {

	private static final long serialVersionUID = 1L;

	private final GeographicArea area;
	private AlertState state;
	private final Map<RemoteGuardianInterface, AlertState> guardians;
	private final RemoteDashBoardInterface dashBoardInstance;
	private final Registry registry;
	private final int patchID;
	private final RemoteObjectFactory factory;

	public Patch(final Point2D topLeftBound, final Point2D bottomRightBound, final int patchID)
			throws RemoteException, NotBoundException, AlreadyBoundException {
		super();
		this.area = new GeographicArea(topLeftBound, bottomRightBound);

		this.patchID = patchID;
		this.factory = RemoteObjectFactory.getInstance();
		this.registry = this.factory.getRegistry();
		this.dashBoardInstance = this.factory.getRemoteDashboardObject(this.registry);

		this.state = AlertState.OK;

		this.guardians = new HashMap<RemoteGuardianInterface, AlertState>();
	}

	public void addGuardians() throws AccessException, RemoteException, NotBoundException {

		this.factory.getRemoteGuardianObject(this.getPatchID(), this.registry).forEach(guardianReference -> {
			this.guardians.put(guardianReference, AlertState.OK);
		});

	}

	@Override
	public synchronized void setInEarlyAlert(final RemoteGuardianInterface guard) {
		if (this.guardians.containsKey(guard)) {
			this.guardians.put(guard, AlertState.EARLY_ALERT);
			try {
				this.dashBoardInstance.notifyEarlyAlertGuardian(this.patchID);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}
		if (this.guardians.values().stream().filter(s -> s.equals(AlertState.EARLY_ALERT))
				.count() > (this.guardians.values().size() / 2)) {
			this.guardians.keySet().forEach(g -> {
				try {
					g.goInAlert();
				} catch (final RemoteException e1) {
					e1.printStackTrace();
				}
			});
			try {
				this.dashBoardInstance.notifyAlertPatch(this.patchID);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void restorePatch() {
		this.state = AlertState.OK;
		this.guardians.keySet().forEach(guardian -> {
			try {
				guardian.restoreState();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public int getPatchID() throws RemoteException {
		return this.patchID;
	}

	@Override
	public GeographicArea getPatchArea() throws RemoteException {
		return this.area;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.area == null) ? 0 : this.area.hashCode());
		result = (prime * result) + ((this.dashBoardInstance == null) ? 0 : this.dashBoardInstance.hashCode());
		result = (prime * result) + ((this.factory == null) ? 0 : this.factory.hashCode());
		result = (prime * result) + ((this.guardians == null) ? 0 : this.guardians.hashCode());
		result = (prime * result) + this.patchID;
		result = (prime * result) + ((this.registry == null) ? 0 : this.registry.hashCode());
		result = (prime * result) + ((this.state == null) ? 0 : this.state.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Patch other = (Patch) obj;
		if (this.area == null) {
			if (other.area != null) {
				return false;
			}
		} else if (!this.area.equals(other.area)) {
			return false;
		}
		if (this.dashBoardInstance == null) {
			if (other.dashBoardInstance != null) {
				return false;
			}
		} else if (!this.dashBoardInstance.equals(other.dashBoardInstance)) {
			return false;
		}
		if (this.factory == null) {
			if (other.factory != null) {
				return false;
			}
		} else if (!this.factory.equals(other.factory)) {
			return false;
		}
		if (this.guardians == null) {
			if (other.guardians != null) {
				return false;
			}
		} else if (!this.guardians.equals(other.guardians)) {
			return false;
		}
		if (this.patchID != other.patchID) {
			return false;
		}
		if (this.registry == null) {
			if (other.registry != null) {
				return false;
			}
		} else if (!this.registry.equals(other.registry)) {
			return false;
		}
		if (this.state != other.state) {
			return false;
		}
		return true;
	}

}
