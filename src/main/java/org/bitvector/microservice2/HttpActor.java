package org.bitvector.microservice2;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.io.Serializable;

public class HttpActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Undertow server = null;

    private void start() {
        RoutingHandler rootHandler = Handlers.routing()
                .add(Methods.HEAD, "/foo", exchange -> {
                    exchange.getResponseSender().close();
                })
                .add(Methods.GET, "/foo", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("foo\n");
                })
                .add(Methods.GET, "/foo/{id}", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("foo" + exchange.getQueryParameters().get("id") + "\n");
                });

        server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0", rootHandler)
                .build();

        server.start();
        log.info("HttpActor Started");
    }

    private void stop() {
        server.stop();
        log.info("HttpActor Stopped");
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof Start) {
            this.start();
        } else if (message instanceof Stop) {
            this.stop();
        } else {
            unhandled(message);
        }
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }

}
