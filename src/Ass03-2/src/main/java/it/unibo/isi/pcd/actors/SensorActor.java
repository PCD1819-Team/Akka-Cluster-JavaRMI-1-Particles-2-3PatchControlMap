package it.unibo.isi.pcd.actors;

import java.awt.geom.Point2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import it.unibo.isi.pcd.msgs.DetectNewVal;
import it.unibo.isi.pcd.msgs.Move;
import it.unibo.isi.pcd.msgs.SensorDataMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;

public class SensorActor extends AbstractActor {
	public static final String ACTOR_NAME = "SensorActor";
	public static final String ACTOR_ROLE = "Sensor";
	public static final String PUBLISH_NAME = "SensorPub";

	private double val;
	private final Point2D position;
	final private static double STEP_SIZE = 25;
	final private static double maxVal = 50;
	final private static double minVal = 0;
	private static int detectingRate = 1500;
	private static int movingRate = 2500;
	final private int id;
	final private Cluster cluster;
	private ActorRef publishSubscribeMediator;
	private final ScheduledExecutorService ses;

	public SensorActor(final Point2D topLeftBound, final Point2D bottomRightBound, final int id) {
		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.cluster = Cluster.get(this.getContext().getSystem());
		this.position = new Point2D.Double(
				ThreadLocalRandom.current().nextDouble(topLeftBound.getX(), bottomRightBound.getX()),
				ThreadLocalRandom.current().nextDouble(topLeftBound.getY(), bottomRightBound.getY()));
		this.val = ThreadLocalRandom.current().nextDouble(SensorActor.minVal, SensorActor.maxVal);
		this.id = id;

	}

	@Override
	public void preStart() throws Exception {
		this.publishSubscribeMediator = DistributedPubSub.get(this.getContext().system()).mediator();
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(DashboardActor.PUBLISH_SHUTDOWN, this.getSelf()),
				this.getSelf());
		this.cluster.subscribe(this.getSelf(), ClusterEvent.initialStateAsEvents(), MemberEvent.class,
				MemberRemoved.class);
		this.ses.scheduleWithFixedDelay(() -> {

			this.getSelf().tell(new DetectNewVal(), this.getSelf());
		}, 0, SensorActor.detectingRate, TimeUnit.MILLISECONDS);
		this.ses.scheduleWithFixedDelay(() -> {

			final int dir = ThreadLocalRandom.current().nextInt(0, Move.Direction.values().length);
			this.getSelf().tell(new Move(Move.Direction.values()[dir]), this.getSelf());
		}, 0, SensorActor.movingRate, TimeUnit.MILLISECONDS);

	}

	@Override
	public void postStop() throws Exception {

		this.cluster.unsubscribe(this.getSelf());
	}

	@Override
	public Receive createReceive() {

		return this.receiveBuilder().match(DetectNewVal.class, content -> {

			this.updateVal();

		}).match(Move.class, content -> {

			final double currentX = this.position.getX();
			final double currentY = this.position.getY();
			switch (content.getDirection()) {
			case LEFT:
				this.position.setLocation(currentX - SensorActor.STEP_SIZE, currentY);
				break;

			case RIGHT:
				this.position.setLocation(currentX + SensorActor.STEP_SIZE, currentY);
				break;

			case DOWN:
				this.position.setLocation(currentX, currentY + SensorActor.STEP_SIZE);
				break;

			case UP:
				this.position.setLocation(currentX, currentY - SensorActor.STEP_SIZE);
				break;

			default:
				break;
			}
		}).match(ShutdownMsg.class, content -> {
			this.cluster.shutdown();
			this.context().system().terminate();
			this.ses.shutdownNow();
			System.exit(0);

		}).build();

	}

	private void updateVal() {
		this.val = ThreadLocalRandom.current().nextDouble(SensorActor.minVal, SensorActor.maxVal);

		final SensorDataMsg msg = new SensorDataMsg(this.id, this.position, this.val);
		this.publishSubscribeMediator.tell(new DistributedPubSubMediator.Publish(SensorActor.PUBLISH_NAME, msg),
				this.getSelf());
	}

	public static Props props(final Point2D topLeftBound, final Point2D bottomRightBound, final int id) {
		return Props.create(SensorActor.class, topLeftBound, bottomRightBound, id)
				.withDispatcher("control-aware-dispatcher").withMailbox("bounded-mailbox");
	}

}
