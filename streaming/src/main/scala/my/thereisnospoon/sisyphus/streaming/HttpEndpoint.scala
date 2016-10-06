package my.thereisnospoon.sisyphus.streaming

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers._
import akka.util.ByteString

object HttpEndpoint extends App {

  implicit val system = ActorSystem("streaming-system")
  implicit val materializer = ActorMaterializer()

  val route =
    path(Slash.?) {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Streaming endpoint</h1>"))
      }
    } ~
    pathPrefix("video" / HexLongNumber) {fileId =>
      get {
        val fileDataSource: Source[ByteString, NotUsed] = Source.fromGraph(new GridFsSource(fileId.toHexString))
        complete(fileDataSource)
      }
    }

  Http().bindAndHandle(route, "localhost", 8080)
}
