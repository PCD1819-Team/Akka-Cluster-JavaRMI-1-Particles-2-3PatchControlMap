package it.unibo.isi.pcd.actors;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import it.unibo.isi.pcd.msgs.AckMessage;
import it.unibo.isi.pcd.msgs.ComputeAvgMsg;
import it.unibo.isi.pcd.msgs.NotifyAlertMsg;
import it.unibo.isi.pcd.msgs.ReCheckAlert;
import it.unibo.isi.pcd.msgs.SensorDataMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.utils.AlertState;
import it.unibo.isi.pcd.utils.Patch;

public class GuardianActor extends AbstractActor {
	public static final String ACTOR_NAME = "GuardianActor";
	public static final String ACTOR_ROLE = "Guardian";
	public static final String PUBLISH_NAME = "GuardianPub";
	public static final String RECEIVE_NAME = "GuardianRec";

	final private Cluster cluster;
	private static final double THRESHOLD = 20;
	private static final long AVG_RATE = 3500;
	private AlertState state;
	private final Map<Integer, Double> sensData;
	private final Patch myPatch;
	private boolean earlyAlert;
	private final ActorRef publishSubscribeMediator;
	private final ScheduledExecutorService scheduleTimer;
	private ScheduledExecutorService ses;

	public GuardianActor(final Patch patch) {
		this.sensData = new HashMap<Integer, Double>();
		this.scheduleTimer = Executors.newSingleThreadScheduledExecutor();
		this.cluster = Cluster.get(this.getContext().getSystem());
		this.state = AlertState.OK;
		this.myPatch = patch;
		this.earlyAlert = false;
		this.publishSubscribeMediator = DistributedPubSub.get(this.getContext().system()).mediator();

	}

	@Override
	public void preStart() throws Exception {
		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.ses.scheduleWithFixedDelay(() -> {

			this.getSelf().tell(new ComputeAvgMsg(), this.getSelf());
		}, 0, GuardianActor.AVG_RATE, TimeUnit.MILLISECONDS);

		this.cluster.subscribe(this.getSelf(), ClusterEvent.initialStateAsEvents(), MemberEvent.class);
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(SensorActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(GuardianActor.RECEIVE_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(DashboardActor.PUBLISH_SHUTDOWN, this.getSelf()),
				this.getSelf());
	}

	@Override
	public void postStop() throws Exception {

		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Unsubscribe(SensorActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.cluster.unsubscribe(this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Unsubscribe(GuardianActor.RECEIVE_NAME, this.getSelf()), this.getSelf());
		this.cluster.unsubscribe(this.getSelf());
	}

	@Override
	public Receive createReceive() {

		return this.receiveBuilder().match(NotifyAlertMsg.class, content -> {
			if (content.getPatch().equals(this.myPatch)) {
				switch (content.getState()) {

				case ALERT:
					this.state = AlertState.ALERT;
					break;
				case OK:

					this.state = AlertState.OK;

					this.publishSubscribeMediator.tell(new DistributedPubSubMediator.Publish(GuardianActor.PUBLISH_NAME,
							new AckMessage(content.getPatch())), this.getSelf());

					break;

				default:
					break;
				}
			}

		}).match(SensorDataMsg.class, content -> {
			if (this.myPatch.contains(content.getPosition())) {
				System.out.println(this.myPatch.getPatchID() + " " + "Sensor data received = " + content.getVal());
				this.sensData.put(content.getSensorID(), content.getVal());
			}

		}).match(ComputeAvgMsg.class, content -> {
			System.out.println(GuardianActor.ACTOR_ROLE + " " + this.context().self() + "Compute AVG received");
			this.computeDataAvg();

		}).match(ReCheckAlert.class, content -> {
			if (this.earlyAlert) {
				this.state = AlertState.EARLY_ALERT;
				final NotifyAlertMsg msg = new NotifyAlertMsg(AlertState.EARLY_ALERT, this.myPatch);
				this.publishSubscribeMediator
						.tell(new DistributedPubSubMediator.Publish(GuardianActor.PUBLISH_NAME, msg), this.getSelf());
				this.earlyAlert = false;
			}

		}).match(ShutdownMsg.class, content -> {
			this.cluster.shutdown();
			this.context().system().terminate();
			this.ses.shutdownNow();
			System.exit(0);

		}).build();
	}

	private void computeDataAvg() {
		if (this.state.equals(AlertState.OK)) {
			final OptionalDouble optAvg = this.sensData.values().stream().mapToDouble(m -> m).average();
			this.sensData.clear();
			double avg = 0.0;
			if (optAvg.isPresent()) {
				avg = optAvg.getAsDouble();
			}
			if (avg > GuardianActor.THRESHOLD) {
				if (!this.earlyAlert) {
					this.earlyAlert = true;
					this.scheduleTimer.schedule(() -> {
						this.getSelf().tell(new ReCheckAlert(), this.getSelf());
					}, 5, TimeUnit.SECONDS);
				}
			} else {
				if (this.earlyAlert) {
					this.earlyAlert = false;
				}
			}

		}

	}

	public static Props props(final Patch patch) {
		return Props.create(GuardianActor.class, patch).withDispatcher("control-aware-dispatcher")
				.withMailbox("bounded-mailbox");
	}

}
