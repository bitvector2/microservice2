package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.RoutingHandler;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.Factory;

import java.io.Serializable;

public class HttpActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SettingsImpl settings = Settings.get(getContext().system());
    private Undertow server;

    public HttpActor() {
        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
        SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

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

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }
}
