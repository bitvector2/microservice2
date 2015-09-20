package org.bitvector.microservice2

import akka.actor.{ActorSystem, Props}
import org.slf4j.LoggerFactory

object Main {
  def main(args: Array[String]) = {
    val logger = LoggerFactory.getLogger(this.getClass.getName)
    logger.info("Starting Actor Initialization")

    val system = ActorSystem("TheTheatre")
    val mgrActor = system.actorOf(Props[MgrActor], "MgrActor")
    mgrActor ! MgrActor.Start()

    logger.info("Finished Actor Initialization")
  }
}
