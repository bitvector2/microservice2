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
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class HttpActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = Settings.get(getContext().system());
    private Undertow server;
    private Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
    private SecurityManager securityManager = factory.getInstance();

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

        SecurityUtils.setSecurityManager(securityManager);

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
            // Header value looks like "Basic c3RldmVsOmZ1Y2tvZmY="
            String[] authorizationHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION).split(" ");
            if (!Objects.equals(authorizationHeader[0].toLowerCase().trim(), Headers.BASIC.toString().toLowerCase())) {
                throw new Exception("Bad Authentication Method");
            }

            // Decoded credentials look like "stevel:fuckoff"
            ByteBuffer buffer = FlexBase64.decode(authorizationHeader[1]);
            String[] credentials = new String(buffer.array(), Charset.forName("utf-8")).split(":");

            // Send to Apache Shiro for assertion
            Subject currentUser = SecurityUtils.getSubject();
            if (!currentUser.isAuthenticated()) {
                UsernamePasswordToken token = new UsernamePasswordToken(credentials[0].trim(), credentials[1].trim());
                token.setRememberMe(true);
                currentUser.login(token);
            }

            log.info("Got logged in: " + credentials[0]);
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
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.logout();
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
