package my.thereisnospoon.sisyphus.uploading.processing

import my.thereisnospoon.sisyphus.uploading.Configuration

trait VideoProcessingComponent {
  this: Configuration =>

  lazy val videoProcessingService: VideoProcessingService = {

    val uploadConfig = config.getConfig("sisyphus.upload")
    new VideoProcessingService(
      uploadConfig.getString("tempFilesFolder"),
      uploadConfig.getString("ffmpegPath"),
      uploadConfig.getString("ffprobePath")
    )
  }
}
