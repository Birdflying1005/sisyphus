package my.thereisnospoon.sisyphus.streaming

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import my.thereisnospoon.sisyphus.streaming.source.local.LocalSourceProvider

object Main extends App {

  implicit val actorSystem = ActorSystem("steaming-server-system")
  implicit val actorMaterializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val sourceProvider =
    new LocalSourceProvider(config.getString("sisyphus.streaming.filesFolderPath"), actorSystem)

  val route = new StreamingRoute(sourceProvider).route

  val host = config.getString("sisyphus.streaming.server.host")
  val port = config.getInt("sisyphus.streaming.server.port")

  Http().bindAndHandle(route, host, port)
}
