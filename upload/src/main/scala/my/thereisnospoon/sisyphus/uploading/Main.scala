package my.thereisnospoon.sisyphus.uploading

import akka.http.scaladsl.Http
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.DuplicationCheckServiceComponent
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent
import my.thereisnospoon.sisyphus.uploading.s3.S3Component

object Main extends App {

  val appContext = new ActorSystemComponent
    with Configuration
    with DuplicationCheckServiceComponent
    with VideoProcessingComponent
    with S3Component
    with UploadRouteComponent

  import appContext._

  val host = config.getString("sisyphus.upload.server.host")
  val port = config.getInt("sisyphus.upload.server.port")

  Http().bindAndHandle(uploadRoute.route, host, port)
}
