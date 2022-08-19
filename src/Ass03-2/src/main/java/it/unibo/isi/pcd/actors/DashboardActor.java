package it.unibo.isi.pcd.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sun.javafx.application.PlatformImpl;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import it.unibo.isi.pcd.msgs.AckMessage;
import it.unibo.isi.pcd.msgs.NotifyAlertMsg;
import it.unibo.isi.pcd.msgs.ReassignPatchesMsg;
import it.unibo.isi.pcd.msgs.SensorDataMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.msgs.StartMsg;
import it.unibo.isi.pcd.utils.Patch;
import it.unibo.isi.pcd.view.MainFXMLController;
import it.unibo.isi.pcd.view.MainView;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DashboardActor extends AbstractActor {

	private final Cluster cluster;
	private final MainView view;
	private final MainFXMLController fxmlC;

	public static final String ACTOR_NAME = "DashBoardActor";
	public static final String ACTOR_ROLE = "DashBoard";
	public static final String PUBLISH_NAME = "DashBoardActorPub";
	public static final String PUBLISH_SHUTDOWN = "DashBoardActorShutdown";
	public static final int MAX_SENSORS = 30;
	public static final int MIN_SENSORS = 3;
	private final List<Integer> listPendigRestoreRequest;

	private final List<Integer> reassignedAlertManager;

	private final List<List<Patch>> patchForAlertManager;
	private final ActorRef publishSubscribeMediator;
	private final ScheduledExecutorService ses;

	public DashboardActor() {
		this.reassignedAlertManager = new ArrayList<>();
		this.patchForAlertManager = new ArrayList<>();
		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.listPendigRestoreRequest = new ArrayList<>();
		this.cluster = Cluster.get(this.getContext().getSystem());
		PlatformImpl.startup(() -> {
		});

		this.view = MainView.getInstance();
		this.fxmlC = this.view.setupViewController();

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
		this.publishSubscribeMediator = DistributedPubSub.get(this.getContext().system()).mediator();
	}

	@Override
	public void preStart() throws Exception {
		this.cluster.subscribe(this.getSelf(), ClusterEvent.initialStateAsEvents(), MemberEvent.class,
				UnreachableMember.class, ReachableMember.class, MemberRemoved.class);
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(SensorActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.fxmlC.setDashBoardActor(this);

		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(AlertManager.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.fxmlC.setDashBoardActor(this);
	}

	@Override
	public void postStop() throws Exception {

		this.cluster.unsubscribe(this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Unsubscribe(SensorActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(AlertManager.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.fxmlC.setDashBoardActor(this);
	}

	@Override
	public Receive createReceive() {
		return this.receiveBuilder().match(SensorDataMsg.class, content -> {
			this.fxmlC.addAndMoveSensors(content);
		}).match(NotifyAlertMsg.class, content -> {
			switch (content.getState()) {
			case ALERT:
				this.fxmlC.AlertPatch(content.getPatch());
				break;
			case EARLY_ALERT:
				this.fxmlC.EarlyAlertGuard(content.getPatch());
				break;
			case OK:
				if (this.getSender().equals(this.getSelf())) {

					this.listPendigRestoreRequest.add(content.getPatch().getPatchID());
					this.publishSubscribeMediator.tell(
							new DistributedPubSubMediator.Publish(DashboardActor.PUBLISH_NAME, content),
							this.getSelf());

				}
				break;
			default:
				break;
			}
		}).match(AckMessage.class, content -> {
			if (this.listPendigRestoreRequest.contains(content.getPatch().getPatchID())) {
				this.fxmlC.restorePatch(content.getPatch().getPatchID());
				this.listPendigRestoreRequest.remove(new Integer(content.getPatch().getPatchID()));
			}
		}).match(MemberUp.class, content -> {
			final Optional<String> opt = content.member().getRoles().stream()
					.filter(str -> str.contains(GuardianActor.ACTOR_ROLE)).findAny();
			if (opt.isPresent()) {
				final String str = opt.get().replaceAll("[^0-9]", "");
				final int patchID = Integer.parseInt(str);
				this.fxmlC.addGuardian(patchID);
			}
		}).match(UnreachableMember.class, content -> {
			final int patchID = this.getGuardianPatchID(content.member());
			if (patchID >= 0) {
				this.fxmlC.disableGuardian(patchID);
			}
		}).match(ReachableMember.class, content -> {
			final int patchID = this.getGuardianPatchID(content.member());
			if (patchID >= 0) {
				this.fxmlC.enableGuardian(patchID);
			}
		}).match(MemberRemoved.class, content -> {
			if (content.member().hasRole(SensorActor.ACTOR_ROLE)) {
				final int sensorsNumber = this.getSensorsNumber(content.member());
				if (sensorsNumber >= 0) {
					this.fxmlC.removeSensors(sensorsNumber);
				}
			} else if (content.member().hasRole(AlertManager.ACTOR_ROLE)) {
				final Spliterator<Member> spliterator = this.cluster.state().getMembers().spliterator();

				final Stream<Member> targetStream = StreamSupport.stream(spliterator, false);

				final String str = content.member().getRoles().stream().findFirst().get().replaceAll("[^0-9]", "");
				final int oldAlertManagerID = Integer.parseInt(str);
				this.reassignedAlertManager.add(oldAlertManagerID);
				int randomedAlertManager = 0;
				do {
					randomedAlertManager = ThreadLocalRandom.current().nextInt(0, this.patchForAlertManager.size() - 1);

				} while (!this.reassignedAlertManager.contains(randomedAlertManager));

				this.publishSubscribeMediator.tell(
						new DistributedPubSubMediator.Publish(DashboardActor.PUBLISH_NAME,
								new ReassignPatchesMsg(
										Collections.unmodifiableList(this.patchForAlertManager).get(oldAlertManagerID),
										randomedAlertManager)),

						this.getSelf());

			} else {
				final int patchID = this.getGuardianPatchID(content.member());
				if (patchID >= 0) {
					this.fxmlC.removeGuardian(patchID);
				}
			}
		}).match(StartMsg.class, content -> {

			final List<Patch> allPatches = content.getPatches();

			for (int i = 1; i <= content.getCols()/* content.getRows() */; i++) {
				final Config configAlert = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
						.withFallback(ConfigFactory
								.parseString(("akka.cluster.roles = [" + AlertManager.ACTOR_ROLE + (i - 1)) + "]"))
						.withFallback(ConfigFactory.load());
				final ActorSystem systemAlert = ActorSystem.create("ClusterSystem", configAlert);

				final List<Patch> subPatchs = allPatches.subList((i - 1) * content.getRows(), i * content.getRows());
				this.patchForAlertManager.add(subPatchs);
				systemAlert.actorOf(AlertManager.props(subPatchs, i - 1), DashboardActor.ACTOR_NAME);

			}

			for (int i = 0; i < ThreadLocalRandom.current().nextInt(DashboardActor.MIN_SENSORS,
					DashboardActor.MAX_SENSORS); i++) {
				final Config configSensors = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
						.withFallback(
								ConfigFactory.parseString("akka.cluster.roles = [" + SensorActor.ACTOR_ROLE + i + "]"))
						.withFallback(ConfigFactory.load());
				final ActorSystem systemSensors = ActorSystem.create("ClusterSystem", configSensors);

				final ActorRef ref = systemSensors.actorOf(
						SensorActor.props(content.getGlobalTopLeftBound(), content.getGlobalBottomRightBound(), i),
						SensorActor.ACTOR_NAME);
			}

		}).match(ShutdownMsg.class, content -> {
			this.publishSubscribeMediator.tell(
					new DistributedPubSubMediator.Publish(DashboardActor.PUBLISH_SHUTDOWN, content), this.getSelf());
			this.cluster.shutdown();
			this.context().system().terminate();
			this.ses.shutdownNow();
			System.exit(0);
		}).build();

	}

	private int getGuardianPatchID(final Member memb) {
		final Optional<String> opt = memb.getRoles().stream().filter(str -> str.contains(GuardianActor.ACTOR_ROLE))
				.findAny();

		if (opt.isPresent()) {
			final String str = opt.get().substring(opt.get().length() - 1);
			final int patchID = Integer.parseInt(str);
			return patchID;
		}
		return -1;
	}

	private int getSensorsNumber(final Member memb) {
		final Optional<String> opt = memb.getRoles().stream().filter(str -> str.contains(SensorActor.ACTOR_ROLE))
				.findAny();

		if (opt.isPresent()) {
			final String str = opt.get().substring(opt.get().length() - 1);
			final int sensorsNumber = Integer.parseInt(str);
			return sensorsNumber;
		}
		return -1;
	}

	public static Props props(final ActorSystem systemDashBoard) {
		return Props.create(DashboardActor.class, DashboardActor::new).withDispatcher("control-aware-dispatcher")
				.withMailbox("bounded-mailbox");
	}

}
