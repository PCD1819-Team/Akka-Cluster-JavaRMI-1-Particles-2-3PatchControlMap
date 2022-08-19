package it.unibo.isi.pcd.actors;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unibo.isi.pcd.events.StartEvent;
import it.unibo.isi.pcd.msgs.AckMsg;
import it.unibo.isi.pcd.msgs.AddParticleMsg;
import it.unibo.isi.pcd.msgs.DoStepMsg;
import it.unibo.isi.pcd.msgs.PauseResumeMsg;
import it.unibo.isi.pcd.msgs.RemoveParticleMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.msgs.StopMsg;

public class ControllerActor extends AbstractActor {
  private final Map<ActorRef, String> actorsReferences;
  private ActorRef viewActor;
  private ActorRef modelActor;
  private int counter;

  private ControllerActor() {
    this.actorsReferences = new HashMap<>();
    this.counter = 0;
  }

  public static Props props() {
    return Props.create(ControllerActor.class, ControllerActor::new)
        .withDispatcher("control-aware-dispatcher").withMailbox("bounded-mailbox");
  }

  @Override
  public void preStart() {
    this.viewActor = this.getContext().actorOf(ViewActor.props(), "ViewActor");
    try {
      super.preStart();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void postStop() {

    try {
      super.postStop();
    } catch (final Exception e) {
      e.printStackTrace();
    }

  }

  private Receive workingContext() {
    return this.receiveBuilder().match(StopMsg.class, content -> {
      this.counter = 0;
      this.modelActor.tell(content, this.getSelf());
      this.actorsReferences.forEach((a, n) -> {
        a.tell(content, this.getSelf());
      });

    }).match(ShutdownMsg.class, content -> {
      this.getContext().system().terminate();
    }).match(PauseResumeMsg.class, content -> {
      this.modelActor.tell(content, this.getSelf());
    }).match(AddParticleMsg.class, content -> {
      this.modelActor.tell(content, this.getSelf());
    }).match(RemoveParticleMsg.class, content -> {
      this.modelActor.tell(content, this.getSelf());
    }).match(AckMsg.class, content -> {
      if (++this.counter >= (this.actorsReferences.size() + 1)) {
        this.context().unbecome();
        this.actorsReferences.clear();
        this.viewActor.tell(content, this.getSelf());
      }
    }).match(DoStepMsg.class, content -> {
      this.modelActor.tell(content, this.getSelf());
    }).build();

  }

  private Receive waitingStartContext() {
    return this.receiveBuilder().match(StartEvent.class, content -> {

//    System.out.println("--2" + Thread.currentThread());
      this.modelActor = this.getContext().actorOf(
          ModelActor.props(content.getNumThreads(), content.getSteps(), this.viewActor),
          "ModelActor");

      for (int i = 0; i < content.getNumThreads(); i++) {
        final ActorRef ref = this.getContext()
            .actorOf(WorkerActor.props(
                i < content.getNumThreads() ? content.getParticlesNumber() / content.getNumThreads()
                    : (content.getParticlesNumber() / content.getNumThreads())
                        + (content.getParticlesNumber() % content.getNumThreads()),
                this.modelActor, content.getXBound(), content.getYBound()), "WorkerActor" + i);
        this.actorsReferences.put(ref, "WorkerActor" + i);

      }

      this.getContext().become(this.workingContext());
    }).match(ShutdownMsg.class, content -> {
      this.getContext().system().terminate();
    }).build();

  }

  @Override
  public Receive createReceive() {
    return this.waitingStartContext();

  }

}
