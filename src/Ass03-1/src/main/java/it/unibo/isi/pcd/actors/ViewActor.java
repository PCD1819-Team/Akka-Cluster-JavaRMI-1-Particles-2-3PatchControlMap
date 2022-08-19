package it.unibo.isi.pcd.actors;

import com.sun.javafx.application.PlatformImpl;

import akka.actor.AbstractActor;
import akka.actor.Props;
import it.unibo.isi.pcd.events.StartEvent;
import it.unibo.isi.pcd.msgs.AckMsg;
import it.unibo.isi.pcd.msgs.AddParticleMsg;
import it.unibo.isi.pcd.msgs.DoStepMsg;
import it.unibo.isi.pcd.msgs.PauseResumeMsg;
import it.unibo.isi.pcd.msgs.RemoveParticleMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.msgs.StopMsg;
import it.unibo.isi.pcd.msgs.UpdateParticlesMsg;
import it.unibo.isi.pcd.view.MainFXMLController;
import it.unibo.isi.pcd.view.MainView;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ViewActor extends AbstractActor {
  private final MainView view;
  private final MainFXMLController fxmlController;

  private ViewActor() {
    PlatformImpl.startup(() -> {
    });
    this.view = MainView.getInstance();
    this.fxmlController = this.view.setupViewController();
    this.fxmlController.setActorRef(this.getSelf());
    Platform.runLater(() -> {
      try {
        final Stage primaryStage = new Stage(StageStyle.DECORATED);
        primaryStage.setTitle("Assignment3");
        this.view.start(primaryStage);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    });

  }

  public static Props props() {
    return Props.create(ViewActor.class, ViewActor::new).withDispatcher("control-aware-dispatcher")
        .withMailbox("bounded-mailbox");
  }

  @Override
  public void preStart() {
    this.fxmlController.setActorRef(this.getSelf());
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

  private Receive waitingStartContext() {
    return this.receiveBuilder().match(StartEvent.class, content -> {
      this.fxmlController.toggleStartStopButton();
      this.getContext().parent().tell(content, this.getSelf());
      this.getContext().become(this.elaborateContext());
    }).match(ShutdownMsg.class, content -> {
      this.getContext().parent().tell(content, this.getSelf());
    }).build();
  }

  private Receive elaborateContext() {
    return this.receiveBuilder().match(StopMsg.class, content -> {
      System.out.println("View Stop");
      this.getContext().parent().tell(content, this.getSelf());
    }).match(UpdateParticlesMsg.class, content -> {
      this.fxmlController.setupCircles(content.getParticles());
      this.fxmlController.setNumStepsLeft(content.getNumStepsLeft());
      this.fxmlController.setCurrentLogicTime(content.getCurrentTime());
    }).match(ShutdownMsg.class, content -> {
      this.getContext().parent().tell(content, this.getSelf());
    }).match(PauseResumeMsg.class, content -> {
      this.getContext().parent().tell(content, this.getSelf());
    }).match(AddParticleMsg.class, content -> {
      this.getContext().getParent().tell(content, this.getSelf());
    }).match(RemoveParticleMsg.class, content -> {
      this.getContext().getParent().tell(content, this.getSelf());
    }).match(AckMsg.class, content -> {
      this.fxmlController.enableAll();
      this.fxmlController.resetStartStopBtn();
      this.fxmlController.clear();
      this.getContext().unbecome();
    }).match(DoStepMsg.class, content -> {
      this.getContext().getParent().tell(content, this.getSelf());
    }).build();
  }

  @Override
  public Receive createReceive() {
    return this.waitingStartContext();
  }

}
