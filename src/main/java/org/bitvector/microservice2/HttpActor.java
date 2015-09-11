package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;
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

// import java.io.BufferedReader;
// import java.io.InputStream;
// import java.io.InputStreamReader;

public class HttpActor extends AbstractActor {
    private Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = null;
    private Undertow server = null;
    private ObjectMapper jsonMapper;

    public HttpActor() {
        settings = Settings.get(getContext().system());
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new DefaultScalaModule());

        receive(ReceiveBuilder
                        .match(Start.class, this::doStart)
                        .match(Stop.class, this::doStop)
                        .matchAny(obj -> log.error("HttpActor received unknown message " + obj.toString()))
                        .build()
        );
    }

    private void doStart(Start msg) {
        RoutingHandler rootHandler = Handlers.routing()
                .add(Methods.GET, "/products", this::doGetAllProducts)
                .add(Methods.GET, "/products/{id}", this::doGetProduct);
        // .add(Methods.PUT, "/products/{id}", this::doUpdateProduct)
        // .add(Methods.POST, "/products", this::doAddProduct)
        // .add(Methods.DELETE, "/products/{id}", this::doDeleteProduct);

        server = Undertow.builder()
                .addHttpListener(settings.LISTEN_PORT(), settings.LISTEN_ADDRESS(), rootHandler)
                .build();

        try {
            server.start();
        } catch (RuntimeException e) {
            log.error("Failed to create HTTP actor: " + e.getMessage());
            getContext().stop(self());
        }
    }

    private void doStop(Stop msg) {
        server.stop();
    }

    private void doGetAllProducts(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch();
        }

        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetAllProducts(), timeout);

        String jsonString = null;
        try {
            DbActor.AllProducts result = (DbActor.AllProducts) Await.result(future, timeout.duration());
            jsonString = jsonMapper.writeValueAsString(result.products());
        } catch (Exception e) {
            log.error("Failed to materialize ProductEntities: " + e.getMessage());
        }

        if (jsonString == null) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().close();
        } else if ("null".equals(jsonString)) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().close();
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(jsonString);
        }
    }

    private void doGetProduct(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch();
        }

        long id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetProduct(id), timeout);

        String jsonString = null;
        try {
            DbActor.AProduct result = (DbActor.AProduct) Await.result(future, timeout.duration());
            jsonString = jsonMapper.writeValueAsString(result.product());
        } catch (Exception e) {
            log.error("Failed to materialize Products: " + e.getMessage());
        }

        if (jsonString == null) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().close();
        } else if ("null".equals(jsonString)) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().close();
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(jsonString);
        }
    }

    /*
    private void doUpdateProduct(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch();
        }

        Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder body = new StringBuilder();

        Products product = new Products(null);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            product = jsonMapper.readValue(body.toString(), Products.class);
        } catch (Exception e) {
            log.error("Failed to read request body: " + e.getMessage());
        }

        if (product != null) {
            // product.id(); FIXME
            ActorSelection dbActorSel = context().actorSelection("../DbActor");
            Future<Object> future = Patterns.ask(dbActorSel, new DbActor.UpdateProduct(product), timeout);
            Boolean result;
            try {
                result = (Boolean) Await.result(future, timeout.duration());
                if (result) {
                    exchange.getResponseSender().close();
                } else {
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().close();
                }
            } catch (Exception e) {
                log.error("Failed to complete operation: " + e.getMessage());
                exchange.setStatusCode(500);
                exchange.getResponseSender().close();
            }
        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().close();
        }
    }

    private void doAddProduct(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch();
        }

        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder body = new StringBuilder();

        Products product = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            product = jsonMapper.readValue(body.toString(), Products.class);
        } catch (Exception e) {
            log.error("Failed to read request body: " + e.getMessage());
        }

        if (product != null) {
            ActorSelection dbActorSel = context().actorSelection("../DbActor");
            Future<Object> future = Patterns.ask(dbActorSel, new DbActor.AddProduct(product), timeout);
            Boolean result;
            try {
                result = (Boolean) Await.result(future, timeout.duration());
                if (result) {
                    exchange.getResponseSender().close();
                } else {
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().close();
                }
            } catch (Exception e) {
                log.error("Failed to complete operation: " + e.getMessage());
                exchange.setStatusCode(500);
                exchange.getResponseSender().close();
            }
        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().close();
        }
    }

    private void doDeleteProduct(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch();
        }

        Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        Products product = new Products(null);
        // product.id(); FIXME
        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.DeleteProduct(product), timeout);
        Boolean result;
        try {
            result = (Boolean) Await.result(future, timeout.duration());
            if (result) {
                exchange.getResponseSender().close();
            } else {
                exchange.setStatusCode(500);
                exchange.getResponseSender().close();
            }
        } catch (Exception e) {
            log.error("Failed to complete operation: " + e.getMessage());
            exchange.setStatusCode(500);
            exchange.getResponseSender().close();
        }
    }
    */

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
