package it.unibo.isi.pcd.remote;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.OptionalDouble;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import it.unibo.isi.pcd.utils.RemoteObjectFactory;

public class Guardian extends java.rmi.server.RemoteObject implements RemoteGuardianInterface, Serializable {

	private static final double threshold = 30;
	private static final int recheckAlertDelay = 5000;
	private static final long serialVersionUID = 1L;

	private AlertState state;
	private final LinkedBlockingQueue<Double> values;
	private final RemotePatchInterface patch;
	private boolean earlyAlertFlag;
	private final int guardID;
	private final int patchID;
	private final RemoteObjectFactory factory;
	private final Registry registry;
	private final ScheduledExecutorService ses;

	public Guardian(final int patchID, final int id) throws RemoteException, NotBoundException {
		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.factory = RemoteObjectFactory.getInstance();
		this.registry = this.factory.getRegistry();
		this.state = AlertState.OK;
		this.values = new LinkedBlockingQueue<>();
		this.patch = this.factory.getRemotePatchObject(patchID, this.registry);
		this.guardID = id;
		this.patchID = patchID;
		this.earlyAlertFlag = false;
		this.ses.scheduleWithFixedDelay(() -> {
			this.computeAVG();
		}, 0, 2500, TimeUnit.MILLISECONDS);
	}

	@Override
	public void sendValue(final double newValue) {
		this.values.offer(newValue);
		System.out.println("PatchID: " + this.patchID + "GuardianID: " + this.guardID + "val: " + newValue);
	}

	private void computeAVG() {
		final OptionalDouble avg = this.values.stream().mapToDouble(x -> x).average();
		if (avg.isPresent()) {
			if (avg.getAsDouble() > Guardian.threshold) {

				if (!this.earlyAlertFlag) {
					this.earlyAlertFlag = true;
					this.ses.schedule(() -> {
						try {
							this.recheckEarlyAlertFlag();
						} catch (final RemoteException e) {
							e.printStackTrace();
						}
					}, Guardian.recheckAlertDelay, TimeUnit.MILLISECONDS);

				}

			} else {
				this.earlyAlertFlag = false;
			}
			this.values.clear();
		}

	}

	private void recheckEarlyAlertFlag() throws RemoteException {
		if (this.earlyAlertFlag) {
			this.state = AlertState.EARLY_ALERT;
			this.patch.setInEarlyAlert(this);
		}
	}

	@Override
	public AlertState getState() {
		return this.state;
	}

	@Override
	public void goInAlert() {
		this.state = AlertState.ALERT;
	}

	@Override
	public void restoreState() {
		this.state = AlertState.OK;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.guardID;
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
		final Guardian other = (Guardian) obj;
		if (this.guardID != other.guardID) {
			return false;
		}
		return true;
	}

	public int getGuardID() {
		return this.guardID;
	}

	public int getPatchID() {
		return this.patchID;
	}

}
