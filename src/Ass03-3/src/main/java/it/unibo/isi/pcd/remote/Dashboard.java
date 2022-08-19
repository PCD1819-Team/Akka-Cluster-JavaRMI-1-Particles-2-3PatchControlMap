package it.unibo.isi.pcd.remote;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.javafx.application.PlatformImpl;

import it.unibo.isi.pcd.utils.EarlyAlertGuardianEvent;
import it.unibo.isi.pcd.utils.Event;
import it.unibo.isi.pcd.utils.GeographicArea;
import it.unibo.isi.pcd.utils.OutOfMapException;
import it.unibo.isi.pcd.utils.RemoteObjectFactory;
import it.unibo.isi.pcd.utils.StartEvent;
import it.unibo.isi.pcd.utils.UpdatePatchEventState;
import it.unibo.isi.pcd.utils.UpdateSensorPosition;
import it.unibo.isi.pcd.utils.notifiable;
import it.unibo.isi.pcd.view.MainFXMLController;
import it.unibo.isi.pcd.view.MainView;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Dashboard implements notifiable, Serializable, RemoteDashBoardInterface {

	private static final long serialVersionUID = 1L;
	private static final int MAX_GUARDS_PER_PATCH = 5;
	private static final int MIN_GUARDS_PER_PATCH = 2;
	private static final int MAX_AVG_SENS_PER_PATCH = 2;
	private static final int MIN_AVG_SENS_PER_PATCH = 1;

	private final MainView view;
	private final MainFXMLController fxmlC;
	private static Dashboard instance;
	private final Set<RemotePatchInterface> remotePatches;
	private final LinkedBlockingQueue<Event> eventQueue;
	private boolean stop;
	private final RemoteObjectFactory factory;
	private final Registry registry;
	private List<GeographicArea> patchesAreas;
	private GeographicArea mapArea;

	private Dashboard(final Registry registry) {
		this.factory = RemoteObjectFactory.getInstance();
		this.registry = registry;
		this.stop = false;
		this.eventQueue = new LinkedBlockingQueue<>();
		PlatformImpl.startup(() -> {
		});

		this.view = MainView.getInstance();
		this.fxmlC = this.view.setupViewController();
		this.remotePatches = new HashSet<>();

		Platform.runLater(() -> {
			try {
				final Stage primaryStage = new Stage(StageStyle.DECORATED);
				this.fxmlC.setStage(primaryStage);
				primaryStage.setTitle("Assignment3");
				this.view.start(primaryStage);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
		this.fxmlC.addDashboard(this);

		new Thread(() -> {
			this.computeEvent();
		}).start();

	}

	private void computeEvent() {
		while (!this.stop) {
			Event ev;
			try {
				ev = this.eventQueue.take();

				switch (ev.getType()) {
				case UPDATE:
					final UpdateSensorPosition upSpos = (UpdateSensorPosition) ev;
					this.updateSensorPosition(upSpos.getSensorID(), upSpos.getPosition());
					break;

				case EARLY_ALERT:
					final EarlyAlertGuardianEvent earlyAl = (EarlyAlertGuardianEvent) ev;
					this.earlyAlertGuardian(earlyAl);
					break;

				case ALERT:
					final UpdatePatchEventState upEvA = (UpdatePatchEventState) ev;
					this.alertPatch(upEvA.getContent());
					break;

				case RESTORE:
					final UpdatePatchEventState upEvR = (UpdatePatchEventState) ev;
					this.restorePatch(upEvR);
					break;

				case START:
					this.start(((StartEvent) ev));
					break;

				case STOP:
					this.shutdown();
					break;

				default:
					break;
				}

			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	private void restorePatch(final UpdatePatchEventState upEv) {
		this.fxmlC.restorePatch(upEv.getContent());
		for (final RemotePatchInterface p : this.remotePatches) {
			try {
				if (p.getPatchID() == upEv.getContent()) {
					p.restorePatch();
				}
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public static Dashboard getInstance(final Registry registry) {
		if (Dashboard.instance == null) {
			Dashboard.instance = new Dashboard(registry);
		}
		return Dashboard.instance;
	}

	@Override
	public void notifyUpdateSensorPosition(final int sensorID, final Point2D position) throws RemoteException {
		this.addEvent(new UpdateSensorPosition(sensorID, position));

	}

	@Override
	public void notifyAlertPatch(final int patchID) throws RemoteException {
		this.addEvent(new UpdatePatchEventState(Event.EventType.ALERT, patchID));
	}

	public void notifyRestorePatch(final int patchID) {
		this.addEvent(new UpdatePatchEventState(Event.EventType.RESTORE, patchID));
	}

	@Override
	public void notifyEarlyAlertGuardian(final int patchID) throws RemoteException {
		this.addEvent(new EarlyAlertGuardianEvent(patchID));
	}

	private void earlyAlertGuardian(final EarlyAlertGuardianEvent earlyAl) {
		this.fxmlC.EarlyAlertGuard(earlyAl.getContent());

	}

	private void alertPatch(final int patchID) {
		this.fxmlC.AlertPatch(patchID);

	}

	private void updateSensorPosition(final int sensorID, final Point2D position) {
		this.fxmlC.addAndMoveSensors(sensorID, position);
	}

	@Override
	public void addEvent(final Event ev) {
		this.eventQueue.add(ev);
	}

	private void start(final StartEvent ev) {

		try {
			final List<Patch> localPatchesReferences = new ArrayList<Patch>();

			for (int i = 0; i < ev.getNumPatches(); i++) {
				final Patch localPatchReference = new Patch(ev.getPatchesBounds().get(i).getFirst(),
						ev.getPatchesBounds().get(i).getSecond(), i);
				this.factory.exportRemotePatchObject(localPatchReference, this.registry);
				localPatchesReferences.add(localPatchReference);
			}

			for (int i = 0; i < ev.getNumPatches(); i++) {
				this.createGuards(i, ThreadLocalRandom.current().nextInt(Dashboard.MIN_GUARDS_PER_PATCH,
						Dashboard.MAX_GUARDS_PER_PATCH));

			}

			for (int i = 0; i < localPatchesReferences.size(); i++) {
				localPatchesReferences.get(i).addGuardians();
			}

			this.patchesAreas = this.fxmlC.getPatchesAreas();
			this.mapArea = new GeographicArea(this.patchesAreas.get(0).getTopLeftBound(),
					this.patchesAreas.get(this.patchesAreas.size() - 1).getBottomRightBound());

			final int avgSensPerPatch = ThreadLocalRandom.current().nextInt(Dashboard.MIN_AVG_SENS_PER_PATCH,
					Dashboard.MAX_AVG_SENS_PER_PATCH);

			for (int i = 0; i < (avgSensPerPatch * this.patchesAreas.size()); i++) {
				new Sensor(i);
			}

		} catch (final RemoteException e) {
			e.printStackTrace();
		} catch (final NotBoundException e) {
			e.printStackTrace();
		} catch (final AlreadyBoundException e) {
			e.printStackTrace();
		} catch (final OutOfMapException e) {

		}

	}

	private void createGuards(final int patchID, final int numGuards)
			throws RemoteException, NotBoundException, AlreadyBoundException {
		for (int g = 0; g < numGuards; g++) {
			final Guardian localGuardianReference = new Guardian(patchID, g);
			this.factory.exportRemoteGuardianObject(localGuardianReference, this.registry);
			this.fxmlC.addGuardian(patchID);
		}

	}

	public void shutdown() {
		this.stop = true;
		System.exit(0);

	}

	@Override
	public GeographicArea getMapArea() {

		return this.mapArea;
	}

	@Override
	public int getRelatedPatchID(final Point2D position) throws OutOfMapException {

		if (this.mapArea.contains(position)) {
			for (int i = 0; i < this.patchesAreas.size(); i++) {
				if (this.patchesAreas.get(i).contains(position)) {
					return i;
				}
			}
		}

		throw new OutOfMapException();
	}

}
