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
import io.undertow.util.Cookies;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
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
            // Collect the subject's principal and credential via HTTP Basic authentication.
            String[] schemeAndValue = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION).split(" ");
            if (!Objects.equals(schemeAndValue[0].toLowerCase().trim(), Headers.BASIC.toString().toLowerCase())) {
                throw new Exception("Bad Authentication Scheme");
            }
            byte[] buffer = Base64.getDecoder().decode(schemeAndValue[1]);
            String[] principleAndCredential = new String(buffer, Charset.forName("utf-8")).split(":");

            // Verify the subject's principal and credential
            Subject currentUser = SecurityUtils.getSubject();
            if (!currentUser.isAuthenticated()) {
                UsernamePasswordToken token = new UsernamePasswordToken(principleAndCredential[0].trim(), principleAndCredential[1].trim());
                token.setRememberMe(true);
                currentUser.login(token);
            }
            // Create a server side session to remember the subject
            Session currentSession = currentUser.getSession();
            currentSession.setTimeout(24 * 3600 * 1000);

            // Build a cookie with a JWT inside.
            Date jwtExpireAt = new Date(System.currentTimeMillis() + (30 * 1000));
            Date cookieExpireAt = new Date(System.currentTimeMillis() + (24 * 3600 * 1000));
            String jwt = Jwts.builder()
                    .setId(currentSession.getId().toString())
                    .setSubject(currentUser.getPrincipal().toString())
                    .setExpiration(jwtExpireAt)
                    .setIssuer(this.getClass().getPackage().getName())
                    .signWith(SignatureAlgorithm.HS512, Base64.getDecoder().decode(settings.SECRET_KEY()))
                    .compact();
            Cookie accessTokenCookie = Cookies.parseSetCookieHeader("access_token" + "=" + jwt + ";")
                    .setExpires(cookieExpireAt)
                            // .setSecure(true)
                    .setHttpOnly(true);

            // Respond to subject with Cookie
            exchange.getResponseCookies().put("0", accessTokenCookie);
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            // Anything goes wrong then reject the subject
            e.printStackTrace();
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, Headers.BASIC.toString() + " " + Headers.REALM + "=" + "Login");
            exchange.getResponseSender().close();
        }
    }

    private void doLogout(HttpServerExchange exchange) {
        try {
            // Get the cookie back
            Cookie accessTokenCookie = exchange.getRequestCookies().get("access_token");

            // Get the JWT back
            Claims claims = Jwts.parser()
                    .setSigningKey(Base64.getDecoder().decode(settings.SECRET_KEY()))
                    .parseClaimsJws(accessTokenCookie.getValue())
                    .getBody();

            // Reacquire subject from server side session
            Serializable sessionId = claims.getId();
            Subject currentUser = new Subject.Builder()
                    .sessionId(sessionId)
                    .buildSubject();
            if (!Objects.equals(currentUser.getPrincipal(), claims.getSubject())) {
                throw new Exception("No matching user");
            }

            // Logout subject and destroy session
            currentUser.logout();
        } catch (Exception e) {
            // Anything goes wrong then reject the subject
            e.printStackTrace();
            exchange.setStatusCode(StatusCodes.FORBIDDEN);
            exchange.getResponseSender().close();
        }
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
