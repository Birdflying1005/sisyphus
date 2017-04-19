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

  } ~ path("video" / Segment) {videoId =>

    logger.debug(s"Starting to stream file $videoId")

    onSuccess(sourceProvider.getFileLength(videoId)) {fileLength: Long =>

      logger.debug(s"File $videoId length is $fileLength")

      optionalHeaderValue(extractRange) {rangeOption =>

        val range = rangeOption.getOrElse(ByteRange(0, fileLength - 1))
        val (startByte, endByte) = range match {
          case Slice(first, last) => (first, last)
          case FromOffset(offset) => (offset, fileLength - 1)
          case Suffix(length) => (fileLength - length, fileLength - 1)
        }
        val contentLength = endByte - startByte + 1

        logger.debug(s"Byte range for file $videoId: $startByte-$endByte/$fileLength")

        complete(
          HttpResponse(
            status = StatusCodes.PartialContent,
            headers = List(
              `Accept-Ranges`(RangeUnits.Bytes),
              `Content-Range`(ContentRange(startByte, endByte, fileLength))),
            entity = HttpEntity(
              contentType = ContentType(MediaTypes.`video/webm`),
              contentLength = contentLength,
              data = sourceProvider.source(videoId, (startByte, endByte)))
          ))
      }
    }
  } ~ path("thumb" / Segment) {videoId =>

    logger.debug(s"Streaming thumbnail for $videoId")

    val thumbnailName = s"$videoId.png"

    onSuccess(sourceProvider.getFileLength(thumbnailName)) {thumbnailLength =>
      complete(
        HttpEntity(
          ContentType(MediaTypes.`image/png`),
          thumbnailLength,
          sourceProvider.source(thumbnailName, (0,thumbnailLength ))
        )
      )
    }
  }
}
