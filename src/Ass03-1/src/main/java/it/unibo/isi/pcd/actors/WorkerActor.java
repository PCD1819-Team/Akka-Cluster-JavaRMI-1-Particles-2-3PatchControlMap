package it.unibo.isi.pcd.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unibo.isi.pcd.model.FunctionalModel;
import it.unibo.isi.pcd.model.Particle;
import it.unibo.isi.pcd.msgs.AckMsg;
import it.unibo.isi.pcd.msgs.ComputedParticlesMsg;
import it.unibo.isi.pcd.msgs.InitParticlesMsg;
import it.unibo.isi.pcd.msgs.RefreshListMsg;
import it.unibo.isi.pcd.msgs.StopMsg;

public class WorkerActor extends AbstractActor {

  private final ActorRef modelRef;
  private final int dim;
  private final int xBound;
  private final int yBound;

  private WorkerActor(final int size, final ActorRef modelRef, final int xBound, final int yBound) {
    this.dim = size;
    this.modelRef = modelRef;
    this.xBound = xBound;
    this.yBound = yBound;

  }

  public static Props props(final int size, final ActorRef modelRef, final int xBound,
      final int yBound) {
    return Props.create(WorkerActor.class, size, modelRef, xBound, yBound)
        .withDispatcher("control-aware-dispatcher").withMailbox("bounded-mailbox");
  }

  @Override
  public void preStart() {
    try {
      super.preStart();
    } catch (final Exception e) {
      e.printStackTrace();
    }

    final List<Particle> particles = new ArrayList<>();

    for (int i = 0; i < this.dim; i++) {

      particles.add(FunctionalModel.generateRandomParticle(this.xBound, this.yBound));
    }

    this.modelRef.tell(new InitParticlesMsg(particles), this.getSelf());

  }

  @Override
  public void postStop() {
    try {
      super.postStop();
    } catch (final Exception e) {
      e.printStackTrace();
    }

  }

  private Receive computingParticles() {
    return this.receiveBuilder().match(RefreshListMsg.class, content -> {
      final Map<Integer, Particle> result = FunctionalModel.computeParticles(
          content.getLowerBound(), content.getUpperBound(), content.getParticles());

      this.modelRef.tell(new ComputedParticlesMsg(result), this.getSelf());

    }).match(StopMsg.class, content -> {
      System.out.println("Worker stop");
      this.context().parent().tell(new AckMsg(), this.getSelf());
      this.getContext().stop(this.getSelf());
    }).build();

  }

  @Override
  public Receive createReceive() {

    return this.computingParticles();

  }

}
