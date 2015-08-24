package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.io.Serializable;

public class HttpActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = null;
    private Undertow server = null;

    public HttpActor() {
        settings = Settings.SettingsProvider.get(getContext().system());

        receive(ReceiveBuilder
                        .match(Start.class, this::start)
                        .match(Stop.class, this::stop)
                        .matchAny(obj -> log.error("HttpActor received unknown message " + obj.toString()))
                        .build()
        );
    }

    private void start(Start msg) {
        RoutingHandler rootHandler = Handlers.routing()
                .add(Methods.GET, "/products", exchange -> {
                    getContext().actorSelection("../DbActor").tell(new DbActor.GetAllProducts(), sender());

                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products\n");
                })
                .add(Methods.GET, "/products/{id}", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + exchange.getQueryParameters().get("id") + "\n");
                })
                .add(Methods.PUT, "/products/{id}", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + exchange.getQueryParameters().get("id") + "\n");
                })
                .add(Methods.POST, "/products", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products\n");
                })
                .add(Methods.DELETE, "/products/{id}", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + exchange.getQueryParameters().get("id") + "\n");
                });

        server = Undertow.builder()
                .addHttpListener(settings.LISTEN_PORT, settings.LISTEN_ADDRESS, rootHandler)
                .build();

        server.start();
    }

    private void stop(Stop msg) {
        server.stop();
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
