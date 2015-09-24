package org.bitvector.microservice2

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

class SettingsImpl(config: Config) extends Extension {
  val LISTEN_ADDRESS = config.getString("service.listen-address")
  val LISTEN_PORT = config.getInt("service.listen-port")
  val SECRET_KEY = config.getString("service.secret-key")
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
  override def lookup() = Settings

  override def createExtension(system: ExtendedActorSystem) = new SettingsImpl(system.settings.config)

  /**
   * Java API: retrieve the Settings extension for the given system.
   */
  override def get(system: ActorSystem): SettingsImpl = super.get(system)
}
