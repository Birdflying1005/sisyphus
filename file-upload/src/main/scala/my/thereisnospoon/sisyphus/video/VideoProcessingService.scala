package my.thereisnospoon.sisyphus.video

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.{Files, Path, Paths}

class VideoProcessingService(
                              tempFolder: String,
                              ffmpegLocation: String,
                              ffprobeLocation: String) {

  private val DurationPattern = """Duration: (\d{2}):(\d{2}):(\d{2})""".r

  def generateThumbnail(pathToVideo: Path): Option[Path] = {

    val name = pathToVideo.getFileName.toString.split("\\.")(0)
    val thumbnailPath = Paths.get(tempFolder, s"$name.png")

    val process = new ProcessBuilder(ffmpegLocation, "-i", pathToVideo.toString, "-s", "320x180", "-ss", "00:00:01",
      "-frames:v", "1", thumbnailPath.toString)

    //TODO: Add logging

    process.start()

    if (Files.exists(thumbnailPath)) {
      Some(thumbnailPath)
    } else {
      None
    }
  }

  def calculateDuration(pathToVideo: Path): Option[Int] = {

    val process = new ProcessBuilder(ffprobeLocation, "-i", pathToVideo.toString)

    val maybeStringWithDuration: Option[String] = new BufferedReader(
      new InputStreamReader(process.start().getErrorStream()))
      .lines()
      .filter(DurationPattern.findFirstIn(_).isDefined)
      .findFirst()
      .map(Some(_)).orElse(None)

    maybeStringWithDuration.map {
      case DurationPattern(hours, minutes, seconds) => hours.toInt * 60 * 60 + minutes.toInt * 60 + seconds.toInt
    }
  }
}
