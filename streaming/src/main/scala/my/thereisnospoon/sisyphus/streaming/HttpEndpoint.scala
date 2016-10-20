package my.thereisnospoon.sisyphus.streaming

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.MediaType.NotCompressible
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.RangeUnits.Bytes
import akka.util.ByteString

import scala.util.{Failure, Success}

object HttpEndpoint extends App {

  implicit val system = ActorSystem("streaming-system")
  implicit val materializer = ActorMaterializer()

  def extractRange: PartialFunction[HttpHeader, Option[ByteRange]] = {
    case r: Range => Some(r.ranges.head)
    case _ => None
  }

  val route =
    path(Slash.?) {
      get {
        complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Streaming endpoint</h1>")))
      }
    } ~
    path("video" / """\w+""".r) {fileId =>
      get {

        headerValue(extractRange) {range =>

          onComplete[Int](GridFsClient.retrieveFileSize(fileId)) {
            case Failure(ex) => complete(HttpResponse(status = StatusCodes.NotFound))

            case Success(fileLength) =>
              println(s"Range header value: $range")
              println(s"File length: $fileLength")
              val fileDataSource: Source[ByteString, _] = Source.fromGraph(new GridFsSource(fileId))
              complete(HttpResponse(status = StatusCodes.PartialContent, headers = List(`Accept-Ranges`(RangeUnits.Bytes)),
                entity = HttpEntity(MediaType.video("webm", NotCompressible, "webm"), contentLength = fileLength, fileDataSource)))
          }
        }
      }
    }

  Http().bindAndHandle(route, "localhost", 8080)
}
