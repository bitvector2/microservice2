package org.bitvector.microservice2;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

import java.io.Serializable;

public class HttpActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Undertow server = null;

    private void start() {
        RoutingHandler fooHandler = Handlers.routing()
                .get("/foo", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("GET foo");
                    }
                })
                .put("/foo", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("PUT foo");
                    }
                })
                .post("/foo", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("POST foo");
                    }
                })
                .delete("/foo", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("DELETE foo");
                    }
                });

        server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(fooHandler)
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
