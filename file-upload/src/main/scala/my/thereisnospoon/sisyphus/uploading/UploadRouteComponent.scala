package my.thereisnospoon.sisyphus.uploading

import my.thereisnospoon.sisyphus.uploading.processing.dupchek.DuplicationCheckServiceComponent
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent

trait UploadRouteComponent {
  this: ActorSystemComponent with Configuration with VideoProcessingComponent with DuplicationCheckServiceComponent =>

  lazy val uploadRoute: UploadRoute = {
    val tempFolder = config.getString("sisyphus.upload.temp-files-folder")
    new UploadRoute(tempFolder, duplicationCheckService, videoProcessingRouter)
  }
}
