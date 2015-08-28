package org.bitvector.microservice2;

import akka.actor.Extension;
import com.typesafe.config.Config;

public class SettingsImpl implements Extension {
    public final String LISTEN_ADDRESS;
    public final Integer LISTEN_PORT;

    public SettingsImpl(Config config) {
        LISTEN_ADDRESS = config.getString("microservice2.listen-address");
        LISTEN_PORT = config.getInt("microservice2.listen-port");
    }

}
