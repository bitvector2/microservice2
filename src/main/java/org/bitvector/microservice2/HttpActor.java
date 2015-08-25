package org.bitvector.microservice2;


import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class HttpActor extends AbstractActor {
    final Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
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
                    context().actorSelection("../DbActor").tell(new DbActor.GetAllProducts(), sender());
                    Future<Object> future = Patterns.ask(context().actorSelection("../DbActor"), new DbActor.GetAllProducts(), timeout);
                    DbActor.AllProducts result = (DbActor.AllProducts) Await.result(future, timeout.duration());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products\n");
                })
                .add(Methods.GET, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    context().actorSelection("../DbActor").tell(new DbActor.GetProductById(id), sender());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + id + "\n");
                })
                .add(Methods.PUT, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + id + "\n");
                })
                .add(Methods.POST, "/products", exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products\n");
                })
                .add(Methods.DELETE, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("products" + id + "\n");
                });

        server = Undertow.builder()
                .addHttpListener(settings.LISTEN_PORT, settings.LISTEN_ADDRESS, rootHandler)
                .build();

        try {
            server.start();
        } catch (RuntimeException e) {
            log.error("Failed to create HTTP actor: " + e.getMessage());
            getContext().stop(self());
        }
    }

    private void stop(Stop msg) {
        server.stop();
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
