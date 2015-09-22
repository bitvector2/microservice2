package org.bitvector.microservice2;

import akka.actor.ActorContext;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ProductCtrl {
    private RoutingHandler routingHandler;
    private ActorSelection dbActorSel;
    private Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    private LoggingAdapter log;
    private ObjectMapper jsonMapper = new ObjectMapper();

    public ProductCtrl(ActorContext context) {
        log = Logging.getLogger(context.system(), this);
        dbActorSel = context.actorSelection("../DbActor");
        jsonMapper.registerModule(new DefaultScalaModule());

        routingHandler = Handlers.routing()
                .add(Methods.GET, "/products", exchange -> exchange.dispatch(this::doGetAllProducts))
                .add(Methods.GET, "/products/{id}", exchange -> exchange.dispatch(this::doGetAProduct))
                .add(Methods.PUT, "/products/{id}", exchange -> exchange.dispatch(this::doUpdateAProduct))
                .add(Methods.POST, "/products", exchange -> exchange.dispatch(this::doAddAProduct))
                .add(Methods.DELETE, "/products/{id}", exchange -> exchange.dispatch(this::doDeleteAProduct));
    }

    public RoutingHandler getRoutingHandler() {
        return routingHandler;
    }

    private void doGetAllProducts(HttpServerExchange exchange) {
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetAllProducts(), timeout);
        sendProductOutput(future, exchange);
    }

    private void doGetAProduct(HttpServerExchange exchange) {
        Long id = Long.parseLong(exchange.getQueryParameters().get("id").getFirst());

        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetAProduct(id), timeout);
        sendProductOutput(future, exchange);
    }

    private void doUpdateAProduct(HttpServerExchange exchange) {
        Long id = Long.parseLong(exchange.getQueryParameters().get("id").getFirst());
        Product product = receiveProductInput(exchange);
        product.id_$eq(id);

        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.UpdateAProduct(product), timeout);
        sendBooleanOutput(future, exchange);
    }

    private void doAddAProduct(HttpServerExchange exchange) {
        Product product = receiveProductInput(exchange);

        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.AddAProduct(product), timeout);
        sendBooleanOutput(future, exchange);
    }

    private void doDeleteAProduct(HttpServerExchange exchange) {
        Long id = Long.parseLong(exchange.getQueryParameters().get("id").getFirst());
        Product product = new Product(id, null);

        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.DeleteAProduct(product), timeout);
        sendBooleanOutput(future, exchange);
    }

    private void sendBooleanOutput(Future<Object> future, HttpServerExchange exchange) {
        Boolean result;
        try {
            result = (Boolean) Await.result(future, timeout.duration());
            if (result) {
                exchange.getResponseSender().close();
            } else {
                log.error("Failed to complete operation.");
                exchange.setStatusCode(404);
                exchange.getResponseSender().close();
            }
        } catch (Exception e) {
            log.error("Failed to complete operation: " + e.getMessage());
            exchange.setStatusCode(500);
            exchange.getResponseSender().close();
        }
    }

    private void sendProductOutput(Future<Object> future, HttpServerExchange exchange) {
        String jsonString = null;
        try {
            Object obj = Await.result(future, timeout.duration());
            if (obj instanceof DbActor.AllProducts) {
                DbActor.AllProducts result = (DbActor.AllProducts) obj;
                jsonString = jsonMapper.writeValueAsString(result.products());
            } else if (obj instanceof DbActor.AProduct) {
                DbActor.AProduct result = (DbActor.AProduct) obj;
                jsonString = jsonMapper.writeValueAsString(result.product());
            }
        } catch (Exception e) {
            log.error("Failed to materialize Product(s): " + e.getMessage());
        }

        if (jsonString == null) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().close();
        } else if ("null".equals(jsonString)) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().close();
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            exchange.getResponseSender().send(jsonString);
        }
    }

    private Product receiveProductInput(HttpServerExchange exchange) {
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder body = new StringBuilder();

        Product product = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            product = jsonMapper.readValue(body.toString(), Product.class);
        } catch (Exception e) {
            log.error("Failed to read request body: " + e.getMessage());
            exchange.setStatusCode(400);
            exchange.getResponseSender().close();
        }
        return product;
    }
}
