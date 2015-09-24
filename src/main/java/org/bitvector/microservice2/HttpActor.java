package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.Cookie;
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
                .add(Methods.GET, "/logout", exchange -> exchange.dispatch(this::doLogout))
                .add(Methods.GET, "/login", exchange -> exchange.dispatch(this::doLogin))
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
                throw new Exception("Bad Authentication Scheme");
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

            // All forms of failure are some Exception.  Make it here and you are logged in and get a JWT Cookie.
            String jwt = Jwts.builder()
                    .setSubject(currentUser.getPrincipal().toString())
                    .signWith(SignatureAlgorithm.HS512, settings.SECRET_KEY())
                    .compact();

            exchange.getResponseHeaders().put(Headers.SET_COOKIE, "access_token=" + jwt + "; " + "HttpOnly; ");
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, Headers.BASIC.toString() + " " + "realm=\"Login Required\"");
            exchange.getResponseSender().close();
        }
    }

    private void doLogout(HttpServerExchange exchange) {
        try {
            Cookie accessToken = exchange.getRequestCookies().get("access_token");

            Claims body = Jwts.parser()
                    .setSigningKey(settings.SECRET_KEY())
                    .parseClaimsJws(accessToken.getValue())
                    .getBody();

        } catch (Exception e) {
            log.error("Exception caught: " + e.getMessage());
        }
//            Subject currentUser = SecurityUtils.getSubject();
//            currentUser.logout();

    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
