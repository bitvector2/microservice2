package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.io.Serializable;

public class ManagerActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef httpActor = context().actorOf(Props.create(HttpActor.class), "HttpActor");
    private ActorRef dbActor = context().actorOf(Props.create(DbActor.class), "DbActor");

    public ManagerActor() {
        receive(ReceiveBuilder
                        .match(Start.class, this::doStart)
                        .match(Stop.class, this::doStop)
                        .matchAny(obj -> log.error("ManagerActor received unknown message " + obj.toString()))
                        .build()
        );
    }

    private void doStart(Start msg) {
        context().watch(httpActor);
        httpActor.tell(new HttpActor.Start(), sender());

        context().watch(dbActor);
        dbActor.tell(new DbActor.Start(), sender());
    }

    private void doStop(Stop msg) {
        httpActor.tell(new HttpActor.Stop(), sender());
        context().unwatch(httpActor);

        dbActor.tell(new DbActor.Stop(), sender());
        context().unwatch(dbActor);
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
