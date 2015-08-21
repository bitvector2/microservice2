package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

public class Director extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public Director() {
        receive(ReceiveBuilder
                        .matchAny(obj -> log.error("Director received unknown message " + obj.toString()))
                        .build()
        );

        ActorRef httpActor = this.getContext().actorOf(Props.create(HttpActor.class), "HttpActor");
        this.getContext().watch(httpActor);
        httpActor.tell(new HttpActor.Start(
                Integer.parseInt(System.getProperty("bitvector.microservice2.listen-port")),
                System.getProperty("bitvector.microservice2.listen-address")), this.sender());

        ActorRef dbActor = this.getContext().actorOf(Props.create(DbActor.class), "DbActor");
        this.getContext().watch(dbActor);
        dbActor.tell(new DbActor.Start(), this.sender());
    }

}
