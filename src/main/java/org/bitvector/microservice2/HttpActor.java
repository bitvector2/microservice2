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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
                    exchange.dispatch(this::handleGetAllProducts);
                })
                .add(Methods.GET, "/products/{id}", exchange -> {
                    exchange.dispatch(this::handleGetProductById);
                })
                .add(Methods.PUT, "/products/{id}", exchange -> {
                    exchange.dispatch(this::handleUpdateProduct);
                })
                .add(Methods.POST, "/products", exchange -> {
                    exchange.dispatch(this::handleAddProduct);
                })
                .add(Methods.DELETE, "/products/{id}", exchange -> {
                    exchange.dispatch(this::handleDeleteProduct);
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

    private void handleGetAllProducts(HttpServerExchange exchange) {
        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetAllProducts(), timeout);

        String jsonString = null;
        try {
            DbActor.AllProducts result = (DbActor.AllProducts) Await.result(future, timeout.duration());
            jsonString = jsonMapper.writeValueAsString(result.getProductEntities());
        } catch (Exception e) {
            log.error("Failed to materialize ProductEntities: " + e.getMessage());
        }

        if (jsonString == null) {
            exchange.setResponseCode(500);
            exchange.getResponseSender().close();
        } else if ("null".equals(jsonString)) {
            exchange.setResponseCode(404);
            exchange.getResponseSender().close();
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(jsonString);
        }
    }

    private void handleGetProductById(HttpServerExchange exchange) {
        Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.GetProductById(id), timeout);

        String jsonString = null;
        try {
            DbActor.AProduct result = (DbActor.AProduct) Await.result(future, timeout.duration());
            jsonString = jsonMapper.writeValueAsString(result.getProductEntity());
        } catch (Exception e) {
            log.error("Failed to materialize ProductEntity: " + e.getMessage());
        }

        if (jsonString == null) {
            exchange.setResponseCode(500);
            exchange.getResponseSender().close();
        } else if ("null".equals(jsonString)) {
            exchange.setResponseCode(404);
            exchange.getResponseSender().close();
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(jsonString);
        }
    }

    private void handleUpdateProduct(HttpServerExchange exchange) {
        Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder body = new StringBuilder();

        ProductEntity product = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            product = jsonMapper.readValue(body.toString(), ProductEntity.class);
        } catch (Exception e) {
            log.error("Failed to read request body: " + e.getMessage());
        }

        if (product != null) {
            product.setId(id);
            ActorSelection dbActorSel = context().actorSelection("../DbActor");
            Future<Object> future = Patterns.ask(dbActorSel, new DbActor.UpdateProduct(product), timeout);
            Boolean result;
            try {
                result = (Boolean) Await.result(future, timeout.duration());
                if (result) {
                    exchange.getResponseSender().close();
                } else {
                    exchange.setResponseCode(500);
                    exchange.getResponseSender().close();
                }
            } catch (Exception e) {
                log.error("Failed to complete operation: " + e.getMessage());
                exchange.setResponseCode(500);
                exchange.getResponseSender().close();
            }
        } else {
            exchange.setResponseCode(400);
            exchange.getResponseSender().close();
        }
    }

    private void handleAddProduct(HttpServerExchange exchange) {
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder body = new StringBuilder();

        ProductEntity product = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            product = jsonMapper.readValue(body.toString(), ProductEntity.class);
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
                    exchange.setResponseCode(500);
                    exchange.getResponseSender().close();
                }
            } catch (Exception e) {
                log.error("Failed to complete operation: " + e.getMessage());
                exchange.setResponseCode(500);
                exchange.getResponseSender().close();
            }
        } else {
            exchange.setResponseCode(400);
            exchange.getResponseSender().close();
        }
    }

    private void handleDeleteProduct(HttpServerExchange exchange) {
        Integer id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        ProductEntity product = new ProductEntity();
        product.setId(id);
        ActorSelection dbActorSel = context().actorSelection("../DbActor");
        Future<Object> future = Patterns.ask(dbActorSel, new DbActor.DeleteProduct(product), timeout);
        Boolean result;
        try {
            result = (Boolean) Await.result(future, timeout.duration());
            if (result) {
                exchange.getResponseSender().close();
            } else {
                exchange.setResponseCode(500);
                exchange.getResponseSender().close();
            }
        } catch (Exception e) {
            log.error("Failed to complete operation: " + e.getMessage());
            exchange.setResponseCode(500);
            exchange.getResponseSender().close();
        }
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
