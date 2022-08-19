package it.unibo.isi.pcd.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unibo.isi.pcd.model.FunctionalModel;
import it.unibo.isi.pcd.model.Particle;
import it.unibo.isi.pcd.msgs.AckMsg;
import it.unibo.isi.pcd.msgs.AddParticleMsg;
import it.unibo.isi.pcd.msgs.ComputedParticlesMsg;
import it.unibo.isi.pcd.msgs.DoStepMsg;
import it.unibo.isi.pcd.msgs.InitParticlesMsg;
import it.unibo.isi.pcd.msgs.PauseResumeMsg;
import it.unibo.isi.pcd.msgs.RefreshListMsg;
import it.unibo.isi.pcd.msgs.RemoveParticleMsg;
import it.unibo.isi.pcd.msgs.StopMsg;
import it.unibo.isi.pcd.msgs.UpdateParticlesMsg;

public class ModelActor extends AbstractActor {

  private int counter;
  private final int parLevel;
  private final List<Particle> particles;
  private final List<ActorRef> workersRef;
  private final ActorRef viewActor;
  private boolean pause;
  private final List<Particle> particlesToAdd;
  private int particlesToRemove;
  private double currentLogicTime;

  private double timeStart;
  private boolean timeStartLogged;

  private final Map<Integer, Particle> computedParticlesMap;
  private transient int numSteps;

  private ModelActor(final int number, final int numSteps, final ActorRef viewActor) {
    this.timeStartLogged = false;
    this.counter = 0;
    this.parLevel = number;
    this.particles = new ArrayList<>();
    this.viewActor = viewActor;
    this.workersRef = new ArrayList<>();
    this.computedParticlesMap = new TreeMap<>();
    this.pause = false;
    this.numSteps = numSteps;
    this.particlesToAdd = new ArrayList<>();
    this.particlesToRemove = 0;
    this.currentLogicTime = 0.0;
  }

  public static Props props(final int parallelismLevel, final int numSteps,
      final ActorRef viewActor) {
    return Props.create(ModelActor.class, parallelismLevel, numSteps, viewActor)
        .withDispatcher("control-aware-dispatcher").withMailbox("bounded-mailbox");
  }

  @Override
  public void preStart() {

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

  private void spreadParticles() {
    final int np = this.particles.size();
    final int nw = this.workersRef.size();
    final int x = np / nw;
    final int r = np % nw;
    final List<Integer> npxw = new ArrayList<>();
    this.workersRef.forEach(y -> {
      npxw.add(x);
    });

    while (r != 0) {
      npxw.set(r, npxw.get(r) + 1);
    }
    int lb = 0;
    int ub;

    for (int i = 0; i < this.workersRef.size(); i++) {
      ub = (lb + npxw.get(i)) - 1;
      this.workersRef.get(i).tell(new RefreshListMsg(ub, lb, new ArrayList<>(this.particles)),
          this.getSelf());
      lb = ub + 1;
    }
  }

  private Receive waitingParticles() {
    return this.receiveBuilder().match(InitParticlesMsg.class, content -> {
      if (this.timeStartLogged == false) {
        this.timeStartLogged = true;
        this.timeStart = System.currentTimeMillis();
      }

      this.particles.addAll(content.getParticles());
      this.workersRef.add(this.getSender());
      if (++this.counter >= this.parLevel) {
        this.counter = 0;
        this.viewActor.tell(new UpdateParticlesMsg(new ArrayList<>(this.particles), this.numSteps,
            this.currentLogicTime), this.getSelf());

        this.getContext().become(this.stepping());
        if (this.numSteps > 0) {
          this.spreadParticles();
          this.numSteps--;
          this.currentLogicTime += FunctionalModel.deltaT;
        } else {
          this.getContext().parent().tell(new StopMsg(), this.getSelf());
        }
      }

    }).match(StopMsg.class, content -> {
      System.out.println("Model Stop 1");
      this.context().parent().tell(new AckMsg(), this.getSelf());
      this.getContext().stop(this.getSelf());

    }).build();
  }

  public Receive stepping() {
    return this.receiveBuilder().match(DoStepMsg.class, content -> {
      if (this.numSteps > 0) {
        this.numSteps--;
        this.currentLogicTime += FunctionalModel.deltaT;
        System.out.println("Step num: " + this.numSteps);
        while (this.particlesToRemove > 0) {
          this.particles.remove(this.particles.size() - 1);
          this.particlesToRemove--;
        }
        if (!this.particlesToAdd.isEmpty()) {
          this.particles.addAll(new ArrayList<>(this.particlesToAdd));
          this.particlesToAdd.clear();
        }
        this.spreadParticles();
      } else {
        this.getContext().parent().tell(new StopMsg(), this.getSelf());
      }

    }).match(ComputedParticlesMsg.class, content -> {
      this.computedParticlesMap.putAll(content.getResult());
      if (++this.counter >= this.workersRef.size()) {
        this.particles.clear();
        for (int i = 0; i < this.computedParticlesMap.size(); i++) {
          this.particles.add(this.computedParticlesMap.get(i));
        }
        this.counter = 0;
        this.computedParticlesMap.clear();
        System.out.println("update view ");

        this.viewActor.tell(new UpdateParticlesMsg(new ArrayList<>(this.particles), this.numSteps,
            this.currentLogicTime), this.getSelf());
        if (!this.pause) {
          this.getSelf().tell(new DoStepMsg(), this.getSelf());
        }
      }

    }).match(StopMsg.class, content -> {
      System.out.println("Model Stop 2");
      this.context().parent().tell(new AckMsg(), this.getSelf());
      this.getContext().stop(this.getSelf());

      System.err.println("\n\n\n\n\n-----------------------------------------------STOP ::  "
          + ((System.currentTimeMillis() - this.timeStart) / 1000)
          + "--------------------------------------------------------\n\n\n\n");
      this.timeStartLogged = false;
    }).match(PauseResumeMsg.class, content -> {
      if (content.getMsgType().equals(PauseResumeMsg.Type.PAUSE)) {
        this.pause = true;
      } else {
        this.pause = false;
        this.getSelf().tell(new DoStepMsg(), this.getSelf());
      }
    }).match(AddParticleMsg.class, content -> {
      this.particlesToAdd
          .add(FunctionalModel.generateRandomParticle(content.getxBound(), content.getyBound()));
    }).match(RemoveParticleMsg.class, content -> {
      this.particlesToRemove++;
    }).build();
  }

  @Override
  public Receive createReceive() {
    return this.waitingParticles();
  }

}
