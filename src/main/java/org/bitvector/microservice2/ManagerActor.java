package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

public class ManagerActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public ManagerActor() {
        receive(ReceiveBuilder
                        .matchAny(obj -> log.error("ManagerActor received unknown message " + obj.toString()))
                        .build()
        );

        ActorRef httpActor = context().actorOf(Props.create(HttpActor.class), "HttpActor");
        context().watch(httpActor);
        httpActor.tell(new HttpActor.Start(), sender());
/*
        ActorRef dbActor = context().actorOf(Props.create(DbActor.class), "DbActor");
        context().watch(dbActor);
        dbActor.tell(new DbActor.Start(), sender());
*/
        ActorRef miscActor = context().actorOf(Props.create(MiscActor.class), "MiscActor");
        context().watch(miscActor);
        miscActor.tell(new MiscActor.Start(), sender());
    }
}
