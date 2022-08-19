package it.unibo.isi.pcd.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.Member;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import it.unibo.isi.pcd.msgs.AckMessage;
import it.unibo.isi.pcd.msgs.NotifyAlertMsg;
import it.unibo.isi.pcd.msgs.ReassignPatchesMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.utils.AlertState;
import it.unibo.isi.pcd.utils.Patch;

public class AlertManager extends AbstractActor {

	final private Cluster cluster;
	final private Map<Patch, AlertState> patchesOfCompetence;
	private final Map<Patch, List<ActorRef>> earlyAlertCounter;
	private ActorRef publishSubscribeMediator;

	public static final String ACTOR_NAME = "AlertManagerActor";
	public static final String ACTOR_ROLE = "AlertManager";
	public static final String PUBLISH_NAME = "AlertPubSub";
	private static final int MAX_GUARDIANS_FOR_PATCH = 4;

	private final ScheduledExecutorService ses;
	private int numGuards;
	private int guardAckForRestore;
	private boolean done;
	private final int alertManagerID;

	public AlertManager(final List<Patch> patchesOfCompetence, final int alertManagerID) {
		this.alertManagerID = alertManagerID;
		this.done = false;
		this.ses = Executors.newSingleThreadScheduledExecutor();
		this.numGuards = 0;
		this.guardAckForRestore = 0;
		this.cluster = Cluster.get(this.getContext().getSystem());
		this.patchesOfCompetence = new HashMap<Patch, AlertState>();
		this.earlyAlertCounter = new HashMap<Patch, List<ActorRef>>();
		patchesOfCompetence.forEach(patch -> {
			this.patchesOfCompetence.put(patch, AlertState.OK);
			this.earlyAlertCounter.put(patch, new ArrayList<ActorRef>());
		});

		patchesOfCompetence.forEach(subPatch -> {
			final Config configGuard = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
					.withFallback(ConfigFactory.parseString(
							"akka.cluster.roles = [" + GuardianActor.ACTOR_ROLE + subPatch.getPatchID() + "]"))
					.withFallback(ConfigFactory.load());
			System.out.print("\n\n\n\n\n" + this.getSelf() + "||" + subPatch.getPatchID() + "\n\n\n\n\n");
			IntStream.rangeClosed(1, ThreadLocalRandom.current().nextInt(1, AlertManager.MAX_GUARDIANS_FOR_PATCH))

					.forEach(num -> {
						final ActorSystem systemGuard = ActorSystem.create("ClusterSystem", configGuard);
						systemGuard.actorOf(GuardianActor.props(subPatch), GuardianActor.ACTOR_NAME);
					});
		});

	}

	@Override
	public void preStart() throws Exception {
		this.cluster.subscribe(this.getSelf(), ClusterEvent.initialStateAsEvents(), MemberEvent.class);
		this.publishSubscribeMediator = DistributedPubSub.get(this.getContext().system()).mediator();
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(GuardianActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(DashboardActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Subscribe(DashboardActor.PUBLISH_SHUTDOWN, this.getSelf()),
				this.getSelf());

	}

	@Override
	public void postStop() throws Exception {
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Unsubscribe(GuardianActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.publishSubscribeMediator.tell(
				new DistributedPubSubMediator.Unsubscribe(DashboardActor.PUBLISH_NAME, this.getSelf()), this.getSelf());
		this.cluster.unsubscribe(this.getSelf());
	}

	@Override
	public Receive createReceive() {

		return this.receiveBuilder().match(NotifyAlertMsg.class, content -> {

			if (this.patchesOfCompetence.containsKey(content.getPatch())) {
				switch (content.getState()) {
				case EARLY_ALERT:
					this.handleEarlyAlert(content.getPatch());
					break;
				case OK:
					this.done = false;
					this.handlePatchRestore(content.getPatch());
					this.scheduleResendRestoreStatus(content);
					break;
				default:
					break;
				}
			}

		}).match(AckMessage.class, content -> {

			if (this.patchesOfCompetence.containsKey(content.getPatch())) {

				if (++this.guardAckForRestore >= this.numGuards) {

					this.publishSubscribeMediator.tell(
							new DistributedPubSubMediator.Publish(AlertManager.PUBLISH_NAME, content), this.getSelf());
					this.guardAckForRestore = 0;
					this.done = true;
				}
			}
		}).match(ShutdownMsg.class, content -> {
			this.cluster.shutdown();
			this.context().system().terminate();
			this.ses.shutdownNow();
			System.exit(0);

		}).match(ReassignPatchesMsg.class, content -> {
			if (this.alertManagerID == content.getTargetID()) {
				content.getPatches().forEach(patch -> {
					this.patchesOfCompetence.put(patch, AlertState.OK);
				});
			}
		}).build();

	}

	private void scheduleResendRestoreStatus(final NotifyAlertMsg content) {
		this.ses.schedule(() -> {
			if (!this.done) {
				this.scheduleResendRestoreStatus(content);
				this.handlePatchRestore(content.getPatch());
			}
		}, 2050, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	private void handleEarlyAlert(final Patch patch) {

		if (!this.earlyAlertCounter.get(patch).contains(this.getContext().sender())) {
			this.earlyAlertCounter.get(patch).add(this.getContext().sender());
			this.numGuards = 0;

			final NotifyAlertMsg earlyAlertMsg = new NotifyAlertMsg(AlertState.EARLY_ALERT, patch);

			this.publishSubscribeMediator.tell(
					new DistributedPubSubMediator.Publish(AlertManager.PUBLISH_NAME, earlyAlertMsg), this.getSelf());
			final Spliterator<Member> spliterator = this.cluster.state().getMembers().spliterator();

			final Stream<Member> targetStream = StreamSupport.stream(spliterator, false);
			targetStream.map(member -> member.getRoles()).filter(setRoles -> {
				return setRoles.stream().filter(singleRole -> singleRole.contains(GuardianActor.ACTOR_ROLE)).findAny()
						.isPresent();
			}).forEach(setFilteredRoles -> {
				final Optional<String> opt = setFilteredRoles.stream().findFirst();
				if (opt.isPresent()) {
					final String str = opt.get().replaceAll("[^0-9]", "");
					final int patchID = Integer.parseInt(str);
					this.numGuards += patchID == patch.getPatchID() ? 1 : 0;
				}
			});

			if (this.earlyAlertCounter.get(patch).size() > (this.numGuards / 2)) {
				final NotifyAlertMsg alertMsg = new NotifyAlertMsg(AlertState.ALERT, patch);

				this.patchesOfCompetence.put(patch, AlertState.ALERT);

				this.publishSubscribeMediator.tell(
						new DistributedPubSubMediator.Publish(GuardianActor.RECEIVE_NAME, alertMsg), this.getSelf());

				this.publishSubscribeMediator.tell(
						new DistributedPubSubMediator.Publish(AlertManager.PUBLISH_NAME, alertMsg), this.getSelf());

			}
		}
	}

	private void handlePatchRestore(final Patch patch) {
		final NotifyAlertMsg msg = new NotifyAlertMsg(AlertState.OK, patch);
		this.patchesOfCompetence.put(patch, AlertState.OK);
		this.publishSubscribeMediator.tell(new DistributedPubSubMediator.Publish(GuardianActor.RECEIVE_NAME, msg),
				this.getSelf());
		this.earlyAlertCounter.get(patch).clear();
	}

	public static Props props(final List<Patch> patchesOfCompetence, final int alertManagerID) {
		return Props.create(AlertManager.class, patchesOfCompetence, alertManagerID)
				.withDispatcher("control-aware-dispatcher").withMailbox("bounded-mailbox");
	}

}
