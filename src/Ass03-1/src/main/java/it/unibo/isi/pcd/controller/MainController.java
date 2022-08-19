package it.unibo.isi.pcd.controller;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unibo.isi.pcd.actors.ControllerActor;

public class MainController {
  public static void main(final String args[]) {
    final ActorSystem system = ActorSystem.create("System-Actor");
    final ActorRef controllerActor = system.actorOf(ControllerActor.props(), "Controller-Actor");
  }
}
