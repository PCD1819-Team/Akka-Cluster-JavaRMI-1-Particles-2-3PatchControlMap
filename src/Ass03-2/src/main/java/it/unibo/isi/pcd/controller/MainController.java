package it.unibo.isi.pcd.controller;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import it.unibo.isi.pcd.actors.DashboardActor;

public class MainController {

	public static void main(final String args[]) {

		final Config configDashboard = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551)
				.withFallback(ConfigFactory.parseString("akka.cluster.roles = [" + DashboardActor.ACTOR_ROLE + "]"))
				.withFallback(ConfigFactory.load());
		final ActorSystem systemDashBoard = ActorSystem.create("ClusterSystem", configDashboard);

		systemDashBoard.actorOf(DashboardActor.props(systemDashBoard), DashboardActor.ACTOR_NAME);
	}

}
