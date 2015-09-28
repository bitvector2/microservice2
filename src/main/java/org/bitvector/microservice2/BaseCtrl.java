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
            // Collect the subject's username and password via HTTP basic authentication.
            String[] schemeAndValue = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION).split(" ");
            if (!Objects.equals(schemeAndValue[0].toLowerCase().trim(), Headers.BASIC.toString().toLowerCase())) {
                throw new Exception("Bad authentication scheme");
            }
            byte[] buffer = Base64.getDecoder().decode(schemeAndValue[1]);
            String[] usernameAndPassword = new String(buffer, Charset.forName("utf-8")).split(":");

            // Verify the subject's username and password
            Subject currentUser = SecurityUtils.getSubject();
            if (!currentUser.isAuthenticated()) {
                UsernamePasswordToken token = new UsernamePasswordToken(usernameAndPassword[0].trim(), usernameAndPassword[1].trim());
                token.setRememberMe(true);
                currentUser.login(token);
            }

            // Create a server side session to remember the subject
            Session currentSession = currentUser.getSession(true);
            currentSession.setTimeout(3600 * 1000); // 1 hour in-activity timeout

            // Build a cookie with a JWT value both having 24 hr lifespan.
            Date jwtExpireAt = new Date(System.currentTimeMillis() + (24 * 3600 * 1000));
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
                    .setHttpOnly(true);

            // Respond to subject with cookie
            exchange.getResponseCookies().put("0", accessTokenCookie);
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            rejectSubject(exchange);
        }
    }

    private void doLogout(HttpServerExchange exchange) {
        try {
            Subject currentUser = verifySubject(exchange);
            currentUser.logout();
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().close();
        } catch (Exception e) {
            rejectSubject(exchange);
        }
    }

    protected Subject verifySubject(HttpServerExchange exchange) throws Exception {
        // Get the cookie back from subject
        Cookie accessTokenCookie = exchange.getRequestCookies().get("access_token");

        // Get the claims back from JWT
        Claims claims = Jwts.parser()
                .setSigningKey(Base64.getDecoder().decode(settings.SECRET_KEY()))
                .parseClaimsJws(accessTokenCookie.getValue())
                .getBody();

        // Load subject from server side session
        Subject currentUser = new Subject.Builder()
                .sessionId(claims.getId())
                .buildSubject();
        if (!Objects.equals(currentUser.getPrincipal(), claims.getSubject())) {
            throw new Exception("No matching subject");
        }
        if (!currentUser.isAuthenticated()) {
            throw new Exception("Subject not logged in");
        }

        return currentUser;
    }

    protected void rejectSubject(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
        exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, Headers.BASIC.toString() + " " + Headers.REALM + "=" + "Login");
        exchange.getResponseSender().close();
    }
}
