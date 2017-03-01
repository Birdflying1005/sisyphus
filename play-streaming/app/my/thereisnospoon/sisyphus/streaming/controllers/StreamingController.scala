package my.thereisnospoon.sisyphus.streaming.controllers

import javax.inject.Inject

import my.thereisnospoon.sisyphus.streaming.source.SourceProvider
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}

import play.api.libs.concurrent.Execution.Implicits._

class StreamingController @Inject() (sourceProvider: SourceProvider) extends Controller {

  def index = Action {
    Ok("Streaming endpoint")
  }

  def streamFile(fileId: String) = Action.async {request =>

    println(request.headers)

    sourceProvider.getFileLength(fileId).map {fileLength =>
      Result(
        header = ResponseHeader(PARTIAL_CONTENT, Map.empty),
        body = HttpEntity.Streamed(sourceProvider.source(fileId, 0 to 1), Some(fileLength), Some("video/webm"))
      )
    }
  }
}
