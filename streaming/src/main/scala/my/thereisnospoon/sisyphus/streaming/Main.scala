package my.thereisnospoon.sisyphus.streaming

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ServerSettings
import com.typesafe.config.ConfigFactory
import my.thereisnospoon.sisyphus.streaming.source.local.LocalSourceProvider

object Main extends App {

  private val actorSystem = ActorSystem("steaming-server-system")
  private val config = ConfigFactory.load()
  private val sourceProvider =
    new LocalSourceProvider(config.getString("sisyphus.streaming.filesFolderPath"), actorSystem)

  new StreamingServer(sourceProvider).startServer(
    config.getString("sisyphus.streaming.server.host"),
    config.getInt("sisyphus.streaming.server.port"),
    ServerSettings(config),
    actorSystem)
}
