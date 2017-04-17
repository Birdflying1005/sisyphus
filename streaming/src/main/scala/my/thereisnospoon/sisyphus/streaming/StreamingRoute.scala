package my.thereisnospoon.sisyphus.streaming

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.ByteRange.{FromOffset, Slice, Suffix}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import my.thereisnospoon.sisyphus.streaming.source.SourceProvider

class StreamingRoute(sourceProvider: SourceProvider) extends LazyLogging {

  private val extractRange: HttpHeader => Option[ByteRange] = {
    case h: Range => Some(h.ranges.head)
    case _ => None
  }

  val route: Route = pathSingleSlash {
    complete {"Streaming endpoint"}

  } ~ path("file" / Segment) {fileId =>
    onSuccess(sourceProvider.getFileLength(fileId)) {fileLength: Long =>
      optionalHeaderValue(extractRange) {rangeOption =>

        val range = rangeOption.getOrElse(ByteRange(0, fileLength - 1))
        val (startByte, endByte) = range match {
          case Slice(first, last) => (first, last)
          case FromOffset(offset) => (offset, fileLength - 1)
          case Suffix(length) => (fileLength - length, fileLength - 1)
        }

        val contentLength = endByte - startByte + 1

        logger.debug(s"Byte range: $startByte-$endByte/$fileLength")

        complete(
          HttpResponse(
            status = StatusCodes.PartialContent,
            headers = List(
              `Accept-Ranges`(RangeUnits.Bytes),
              `Content-Range`(ContentRange(startByte, endByte, fileLength))),
            entity = HttpEntity(
              contentType = ContentType(MediaTypes.`video/webm`),
              contentLength = contentLength,
              data = sourceProvider.source(fileId, (startByte, endByte)))
          ))
      }
    }
  }
}
