package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class HttpActor extends AbstractActor {
    private Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = null;
    private Undertow server = null;
    private ObjectMapper jsonMapper;

    public HttpActor() {
        settings = Settings.SettingsProvider.get(getContext().system());
        jsonMapper = new ObjectMapper();
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
                    ActorSelection dbActorSel = context().actorSelection("../DbActor");
                    Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetAllProducts(), timeout);
                    DbActor.AllProducts result = (DbActor.AllProducts) Await.result(future, timeout.duration());
                    String jsonString = jsonMapper.writeValueAsString(result.getProductEntities());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    exchange.getResponseSender().send(jsonString);
                })
                .add(Methods.GET, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    ActorSelection dbActorSel = context().actorSelection("../DbActor");
                    Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetProductById(id), timeout);
                    DbActor.AProduct result = (DbActor.AProduct) Await.result(future, timeout.duration());
                    String jsonString = jsonMapper.writeValueAsString(result.getProductEntity());
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    exchange.getResponseSender().send(jsonString);
                })
                .add(Methods.PUT, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    ProductEntity product = jsonMapper.readValue(getRequestBody(exchange), ProductEntity.class);
                    product.setId(id);

                    ActorSelection dbActorSel = context().actorSelection("../DbActor");
                    dbActorSel.tell(new DbActor.UpdateProduct(product), sender());
                    exchange.getResponseSender().close();
                })
                .add(Methods.POST, "/products", exchange -> {
                    ProductEntity product = jsonMapper.readValue(getRequestBody(exchange), ProductEntity.class);

                    ActorSelection dbActorSel = context().actorSelection("../DbActor");
                    dbActorSel.tell(new DbActor.AddProduct(product), sender());
                    exchange.getResponseSender().close();
                })
                .add(Methods.DELETE, "/products/{id}", exchange -> {
                    Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
                    ActorSelection dbActorSel = context().actorSelection("../DbActor");
                    dbActorSel.tell(new DbActor.DeleteProductById(id), sender());
                    exchange.getResponseSender().close();
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

    private String getRequestBody(HttpServerExchange exchange) {
        // FIXME
        String body = exchange.getRequestChannel().toString();
        return body;
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
