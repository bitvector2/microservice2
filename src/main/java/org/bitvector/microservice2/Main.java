package org.bitvector.microservice2;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class.getName());
        logger.info("Starting Initialization");

        String propertiesFile = "microservice2.properties";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream resourceStream = loader.getResourceAsStream(propertiesFile)) {
            if (resourceStream != null) {
                Properties props = new Properties(System.getProperties());
                props.load(resourceStream);
                System.setProperties(props);
            } else {
                logger.error("Could not load properties file: " + propertiesFile);
            }
        } catch (IOException e) {
            logger.error("Could not load properties file: " + propertiesFile, e);
        }

        ActorSystem system = ActorSystem.create("TheTheatre");
        ActorRef httpActor = system.actorOf(Props.create(HttpActor.class), "HttpActor");
        httpActor.tell(new HttpActor.Start(), ActorRef.noSender());

        logger.info("Finished Initialization");
    }
}
