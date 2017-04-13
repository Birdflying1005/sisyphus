package my.thereisnospoon.sisyphus.uploading.processing.video

import java.nio.file.{Files, Paths}

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class VideoProcessingServiceTest extends FlatSpec with Matchers {

  val uploadConfig = ConfigFactory.load().getConfig("sisyphus.upload")

  val testedInstance = new VideoProcessingService(
    uploadConfig.getString("temp-files-folder"),
    uploadConfig.getString("video-processing.ffmpeg-path"),
    uploadConfig.getString("video-processing.ffprobe-path")
  )

  val testVideoPath = Paths.get("../test.webm")
  val testVideoDuration = 32

  "VideoProcessingService" should "calculate duration of video" in {
    testedInstance.calculateDuration(testVideoPath) shouldEqual Some(testVideoDuration)
  }

  it should "create thumbnail for video" in {
    val thumbnailPath = testedInstance.generateThumbnail(testVideoPath) match {
      case Some(path) => path
      case None => fail("Thumbnail wasn't generated")
    }
    Files.exists(thumbnailPath) shouldBe true
    Files.delete(thumbnailPath)
  }
}
