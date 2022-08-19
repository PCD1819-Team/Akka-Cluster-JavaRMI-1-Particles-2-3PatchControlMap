package it.unibo.isi.pcd.remote;

import java.awt.geom.Point2D;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import it.unibo.isi.pcd.utils.Direction;
import it.unibo.isi.pcd.utils.GeographicArea;
import it.unibo.isi.pcd.utils.OutOfMapException;
import it.unibo.isi.pcd.utils.RemoteObjectFactory;

public class Sensor {

	private static final long MOVING_RATE = 2000;
	private static final long DETECTION_RATING = 1000;
	private final Point2D position;
	private double value;
	private final int sensorID;
	private final ScheduledExecutorService ses;
	private List<RemoteGuardianInterface> guardians;
	private final RemoteDashBoardInterface dashBoardInstance;
	private final Registry registry;
	private static final double MIN_VALUE = 0;
	private static final double MAX_VALUE = 100;

	final private static double STEP_SIZE = 25;
	private final RemoteObjectFactory factory;

	private final GeographicArea mapArea;

	private RemotePatchInterface patch;
	private int currentPatchID;
	private GeographicArea currentPatchArea;
	private boolean outOfMap;

	public Sensor(final int sensorID)
			throws RemoteException, NotBoundException, AlreadyBoundException, OutOfMapException {

		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.value = 0.0;
		this.factory = RemoteObjectFactory.getInstance();
		this.registry = this.factory.getRegistry();
		this.sensorID = sensorID;

		this.dashBoardInstance = this.factory.getRemoteDashboardObject(this.registry);
		this.mapArea = this.dashBoardInstance.getMapArea();

		final double newX = ThreadLocalRandom.current().nextDouble(this.mapArea.getTopLeftBound().getX(),
				this.mapArea.getBottomRightBound().getX());
		final double newY = ThreadLocalRandom.current().nextDouble(this.mapArea.getTopLeftBound().getY(),
				this.mapArea.getBottomRightBound().getY());

		this.position = new Point2D.Double(newX, newY);
		this.outOfMap = false;
		this.setPatchInfo();

		this.ses.scheduleWithFixedDelay(() -> {
			this.value = ThreadLocalRandom.current().nextDouble(Sensor.MIN_VALUE, Sensor.MAX_VALUE);
			this.guardians.forEach(guardian -> {
				try {
					guardian.sendValue(this.value);
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			});
		}, 0, Sensor.DETECTION_RATING, TimeUnit.MILLISECONDS);

		this.ses.scheduleWithFixedDelay(() -> {

			try {
				this.doStep(ThreadLocalRandom.current().nextInt(0, 3));
			} catch (final RemoteException e) {
				e.printStackTrace();
			} catch (final NotBoundException e) {

			} catch (final OutOfMapException e) {

			}
		}, 0, Sensor.MOVING_RATE, TimeUnit.MILLISECONDS);
	}

	private void doStep(final int dirIndex) throws RemoteException, OutOfMapException, NotBoundException {

		final double currentX = this.position.getX();
		final double currentY = this.position.getY();
		switch (Direction.values()[dirIndex]) {
		case LEFT:
			this.position.setLocation(currentX - Sensor.STEP_SIZE, currentY);
			break;

		case RIGHT:
			this.position.setLocation(currentX + Sensor.STEP_SIZE, currentY);
			break;

		case DOWN:
			this.position.setLocation(currentX, currentY + Sensor.STEP_SIZE);
			break;

		case UP:
			this.position.setLocation(currentX, currentY - Sensor.STEP_SIZE);
			break;

		default:
			break;
		}

		try {
			this.dashBoardInstance.notifyUpdateSensorPosition(this.sensorID, this.position);
		} catch (final RemoteException e) {

			e.printStackTrace();
		}

		if (!this.outOfMap) {
			if (!this.currentPatchArea.contains(this.position)) {
				if (!this.mapArea.contains(this.position)) {
					this.outOfMap = true;
				}
				this.setPatchInfo();
			}
		} else {
			if (this.mapArea.contains(this.position)) {
				this.outOfMap = false;
				this.setPatchInfo();
			}
		}

	}

	private void setPatchInfo() throws OutOfMapException, AccessException, RemoteException, NotBoundException {
		if (!this.outOfMap) {
			this.currentPatchID = this.dashBoardInstance.getRelatedPatchID(this.position);

			this.patch = this.factory.getRemotePatchObject(this.currentPatchID, this.registry);

			this.currentPatchArea = this.patch.getPatchArea();
			this.guardians = this.factory.getRemoteGuardianObject(this.currentPatchID, this.registry);
		} else {
			this.currentPatchID = -1;
			this.patch = null;
			this.currentPatchArea = null;
			this.guardians = null;
		}
	}

	public void addGuardians() throws AccessException, RemoteException, NotBoundException {

		this.factory.getRemoteGuardianObject(this.currentPatchID, this.registry).forEach(guardianReference -> {
			this.guardians.add(guardianReference);
		});

	}

}
