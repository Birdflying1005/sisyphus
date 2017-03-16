package my.thereisnospoon.sisyphus.upload

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer

object UploadServer extends HttpApp with App {

  implicit val system = ActorSystem("upload-endpoint-system")
  implicit val materializer = ActorMaterializer()

  def route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Hi!"))
      }
    }

  startServer("localhost", 9191)
}
