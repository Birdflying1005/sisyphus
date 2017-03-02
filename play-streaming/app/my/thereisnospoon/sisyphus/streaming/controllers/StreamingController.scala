package my.thereisnospoon.sisyphus.streaming.controllers

import javax.inject.Inject

import my.thereisnospoon.sisyphus.streaming.source.SourceProvider
import play.api.http.HttpEntity
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller, ResponseHeader, Result}

class StreamingController @Inject() (sourceProvider: SourceProvider) extends Controller {

  private val RangeHeaderPattern = """bytes=(\d+)\-(\d+)?""".r

  def index = Action {
    Ok("Streaming endpoint")
  }

  def streamFile(fileId: String) = Action.async {request =>

    sourceProvider.getFileLength(fileId).map {fileLength =>

      val range: (Long, Long) = request.headers.get(RANGE).map {
        case RangeHeaderPattern(start, null) => (start.toLong, fileLength)
        case RangeHeaderPattern(start, end) => (start.toLong, end.toLong)

      }.getOrElse((0L, fileLength))

      val (startOfRange, endOfRange) = range
      val dataSource = sourceProvider.source(fileId, range)

      Result(
        header = ResponseHeader(PARTIAL_CONTENT, Map.empty),
        body = HttpEntity.Streamed(dataSource, Some(endOfRange - startOfRange), Some("video/webm"))
      ).withHeaders(
        ACCEPT_RANGES -> "bytes",
        CONTENT_RANGE -> s"bytes $startOfRange-$endOfRange/$fileLength")
    }
  }
}
