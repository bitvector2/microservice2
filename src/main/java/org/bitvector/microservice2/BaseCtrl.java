package org.bitvector.microservice2;

import akka.actor.ActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Cookies;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

public class BaseCtrl {
    protected RoutingHandler routingHandler;
    protected LoggingAdapter log;
    protected SettingsImpl settings;

    public BaseCtrl(ActorContext context) {
        log = Logging.getLogger(context.system(), this);
        settings = Settings.get(context.system());

        routingHandler = Handlers.routing()
                .add(Methods.GET, "/logout", exchange -> exchange.dispatch(this::doLogout))
                .add(Methods.GET, "/login", exchange -> exchange.dispatch(this::doLogin));
    }

    public RoutingHandler getRoutingHandler() {
        return routingHandler;
    }

    private void doLogin(HttpServerExchange exchange) {
        try {
            // Assert SSL was used on frontend
            String xForwardedProtoHeader = exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PROTO);
            if (xForwardedProtoHeader == null) {
                throw new BadBasicAuth("No x-forwarded-proto header");
            }
            if (!Objects.equals(xForwardedProtoHeader.toLowerCase().trim(), "https")) {
                throw new BadBasicAuth("No https encryption");
            }

            // Collect the subject's username and password via HTTP xBasic scheme.
            String authorizationHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (authorizationHeader == null) {
                throw new BadBasicAuth("No HTTP authorization header");
            }
            String[] schemeAndValue = authorizationHeader.split(" ");
            if (schemeAndValue.length != 2) {
                throw new BadBasicAuth("Bad HTTP authorization header");
            }
            if (!Objects.equals(schemeAndValue[0].toLowerCase().trim(), "x" + Headers.BASIC.toString().toLowerCase())) {
                throw new BadBasicAuth("Bad authentication scheme");
            }
            byte[] buffer = Base64.getDecoder().decode(schemeAndValue[1]);
            String[] usernameAndPassword = new String(buffer, Charset.forName("utf-8")).split(":");
            if (usernameAndPassword.length != 2) {
                throw new BadBasicAuth("Bad authentication parameter");
            }

            // Verify the subject's username and password with Shiro
            Subject currentSubject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(usernameAndPassword[0].trim(), usernameAndPassword[1].trim());
            token.setRememberMe(true);
            currentSubject.login(token);

            // Create a server side session to remember the subject
            Session currentSession = currentSubject.getSession(true);
            currentSession.setTimeout(3600 * 1000); // 1 hour in-activity timeout

            // Build a cookie with a JWT value both having 24 hr lifespan.
            Date jwtExpireAt = new Date(System.currentTimeMillis() + (24 * 3600 * 1000));
            Date cookieExpireAt = new Date(System.currentTimeMillis() + (24 * 3600 * 1000));
            String jwt = Jwts.builder()
                    .setId(currentSession.getId().toString())
                    .setSubject(currentSubject.getPrincipal().toString())
                    .setExpiration(jwtExpireAt)
                    .setIssuer(this.getClass().getPackage().getName())
                    .signWith(SignatureAlgorithm.HS512, Base64.getDecoder().decode(settings.SECRET_KEY()))
                    .compact();
            currentSession.setAttribute("jwt", jwt);
            Cookie accessTokenCookie = Cookies.parseSetCookieHeader("access_token" + "=" + jwt + ";")
                    .setExpires(cookieExpireAt)
                    .setSecure(true)
                    .setHttpOnly(true);

            // Respond to subject with cookie
            exchange.getResponseCookies().put("0", accessTokenCookie);
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            // FIXME - need to break out and handle specific exceptions accordingly - maybe
            log.error("Caught exception: " + e.getMessage());
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "x" + Headers.BASIC.toString() + " " + Headers.REALM + "=" + "Login");
            exchange.getResponseSender().close();
        }
    }

    private void doLogout(HttpServerExchange exchange) {
        try {
            Subject currentSubject = verifySubject(exchange);
            currentSubject.logout();

            String jwt = currentSubject.getSession().getAttribute("jwt").toString();
            Date cookieExpireAt = new Date(1000);
            Cookie accessTokenCookie = Cookies.parseSetCookieHeader("access_token" + "=" + jwt + ";")
                    .setExpires(cookieExpireAt)
                    .setSecure(true)
                    .setHttpOnly(true);

            // Respond to subject with nullified cookie
            exchange.getResponseCookies().put("0", accessTokenCookie);
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            rejectSubject(exchange, e);
        }
    }

    protected Subject verifySubject(HttpServerExchange exchange) throws Exception {
        // Assert SSL was used on frontend
        String xForwardedProtoHeader = exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PROTO);
        if (xForwardedProtoHeader == null) {
            throw new BadTokenAuth("No x-forwarded-proto header");
        }
        if (!Objects.equals(xForwardedProtoHeader.toLowerCase().trim(), "https")) {
            throw new BadTokenAuth("No https encryption");
        }

        // Verify Authorization header not set
        String authorizationHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if (authorizationHeader != null) {
            throw new BadTokenAuth("Authorization header present");
        }

        // Get the cookie back from subject
        Cookie accessTokenCookie = exchange.getRequestCookies().get("access_token");
        if (accessTokenCookie == null) {
            throw new BadTokenAuth("No access_token cookie");
        }

        // Get the claims back from JWT
        Claims claims = Jwts.parser()
                .setSigningKey(Base64.getDecoder().decode(settings.SECRET_KEY()))
                .parseClaimsJws(accessTokenCookie.getValue())
                .getBody();

        // Load subject from server side session
        Subject currentSubject = new Subject.Builder()
                .sessionId(claims.getId())
                .buildSubject();
        if (!Objects.equals(currentSubject.getPrincipal(), claims.getSubject())) {
            throw new BadTokenAuth("No matching subject found");
        }

        return currentSubject;
    }

    protected void rejectSubject(HttpServerExchange exchange, Exception e) {
        // FIXME - need to break out and handle specific exceptions accordingly - maybe
        log.error("Caught exception: " + e.getMessage());
        exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
        exchange.getResponseHeaders().put(Headers.LOCATION, "/login");
        exchange.getResponseSender().close();
    }

    public static class BadBasicAuth extends Exception {
        public BadBasicAuth(String message) {
            super(message);
        }
    }

    public static class BadTokenAuth extends Exception {
        public BadTokenAuth(String message) {
            super(message);
        }
    }
}
