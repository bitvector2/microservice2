package org.bitvector.microservice2;

import akka.actor.ActorSystem;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class.getName());
        logger.info("Starting Actor Initialization");

        ActorSystem system = ActorSystem.create("TheTheatre");
        system.actorOf(Props.create(ManagerActor.class), "ManagerActor");

        logger.info("Finished Actor Initialization");
    }
}
