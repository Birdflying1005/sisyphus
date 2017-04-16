package my.thereisnospoon.sisyphus.uploading

import my.thereisnospoon.sisyphus.uploading.processing.dupchek.DuplicationCheckServiceComponent
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent
import my.thereisnospoon.sisyphus.uploading.s3.S3Component

trait UploadRouteComponent {
  this: ActorSystemComponent
    with Configuration
    with VideoProcessingComponent
    with DuplicationCheckServiceComponent
    with S3Component =>

  lazy val uploadRoute: UploadRoute = {
    val tempFolder = config.getString("sisyphus.upload.temp-files-folder")
    new UploadRoute(tempFolder, duplicationCheckService, videoProcessingRouter, s3SinkProvider)
  }
}
