package org.bitvector.microservice2

import akka.actor.{Actor, ActorLogging, Props}

object MgrActor {

  case class Start()

  case class Stop()

}

class MgrActor extends Actor with ActorLogging {

  import MgrActor._

  val settings = Settings(context.system)
  val httpActor = context.actorOf(Props[HttpActor], "HttpActor")
  val dbActor = context.actorOf(Props[DbActor], "DbActor")

  def receive = {
    case Start() => this.doStart()
    case Stop() => this.doStop()
    case _ => log.error("MgrActor received unknown message")
  }

  def doStart() = {
    log.info("MgrActor received start")

    context.watch(httpActor)
    httpActor ! new HttpActor.Start()

    context.watch(dbActor)
    dbActor ! DbActor.Start()
  }

  def doStop() = {
    log.info("MgrActor received stop")

    httpActor ! new HttpActor.Stop()
    context.unwatch(httpActor)

    dbActor ! DbActor.Stop()
    context.unwatch(dbActor)
  }
}
