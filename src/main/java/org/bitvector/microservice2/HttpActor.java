package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.FlexBase64;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class HttpActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = Settings.get(getContext().system());
    private Undertow server;

    public HttpActor() {
        receive(ReceiveBuilder
                        .match(Start.class, this::doStart)
                        .match(Stop.class, this::doStop)
                        .matchAny(obj -> log.error("HttpActor received unknown message " + obj.toString()))
                        .build()
        );
    }

    private void doStart(Start msg) {
        log.info("HttpActor received start");

        ProductCtrl productCtrl = new ProductCtrl(getContext());

        RoutingHandler rootHandler = Handlers.routing()
                .add(Methods.GET, "/login", exchange -> exchange.dispatch(this::doLogin))
                .add(Methods.GET, "/logout", exchange -> exchange.dispatch(this::doLogout))
                .addAll(productCtrl.getRoutingHandler());

        server = Undertow.builder()
                .addHttpListener(settings.LISTEN_PORT(), settings.LISTEN_ADDRESS(), rootHandler)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .build();

        try {
            server.start();
        } catch (RuntimeException e) {
            log.error("Failed to create HTTP actor: " + e.getMessage());
            getContext().stop(self());
        }
    }

    private void doStop(Stop msg) {
        log.info("HttpActor received stop");

        server.stop();
    }

    private void doLogin(HttpServerExchange exchange) {
        try {
            String[] authorizationHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION).split(" ");
            if (!Objects.equals(authorizationHeader[0].toLowerCase().trim(), Headers.BASIC.toString().toLowerCase())) {
                throw new Exception("Bad Authentication Method");
            }

            ByteBuffer buffer = FlexBase64.decode(authorizationHeader[1]);
            String[] credentials = new String(buffer.array(), Charset.forName("utf-8")).split(":");
            log.info("'" + credentials[0] + "'" + " " + "'" + credentials[1] + "'");
            if (!Objects.equals(credentials[0].trim(), "stevel")) {
                throw new Exception("Bad User Name");
            }
            if (!Objects.equals(credentials[1].trim(), "stevel")) {
                throw new Exception("Bad Password");
            }

            log.info("Got logged in: " + credentials[0]);
            // Do other stuff?
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            log.error("Got kicked out: " + e.getMessage());
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, Headers.BASIC.toString() + " " + "realm=\"Login Required\"");
            exchange.getResponseSender().close();
        }
    }

    private void doLogout(HttpServerExchange exchange) {

    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
