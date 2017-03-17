package my.thereisnospoon.sisyphus.video

import my.thereisnospoon.sisyphus.Configuration

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
