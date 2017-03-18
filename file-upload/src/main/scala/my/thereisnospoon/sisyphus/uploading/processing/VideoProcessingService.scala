package my.thereisnospoon.sisyphus.uploading.processing

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.{Files, Path, Paths}
import java.util.Optional
import java.util.stream.Collectors

import com.typesafe.scalalogging.Logger

class VideoProcessingService(
                              tempFolder: String,
                              ffmpegLocation: String,
                              ffprobeLocation: String) {

  private val log = Logger(classOf[VideoProcessingService])

  private val DurationPattern = """Duration: (\d{2}):(\d{2}):(\d{2})""".r

  def generateThumbnail(pathToVideo: Path): Option[Path] = {

    val name = pathToVideo.getFileName.toString.split("\\.")(0)
    val thumbnailPath = Paths.get(tempFolder, s"$name.png")

    val processBuilder = new ProcessBuilder(ffmpegLocation, "-i", pathToVideo.toString, "-s", "320x180", "-ss", "00:00:01",
      "-frames:v", "1", thumbnailPath.toString)

    val process = processBuilder.start()

    log.debug(s"${new BufferedReader(new InputStreamReader(process.getErrorStream))
      .lines()
      .collect(Collectors.joining("\n"))}")

    if (Files.exists(thumbnailPath)) {
      Some(thumbnailPath)
    } else {
      None
    }
  }

  def calculateDuration(pathToVideo: Path): Option[Int] = {

    val processBuilder = new ProcessBuilder(ffprobeLocation, "-i", pathToVideo.toString)

    val maybeStringWithDuration: Optional[String] = new BufferedReader(
      new InputStreamReader(processBuilder.start().getErrorStream))
      .lines()
      .filter(DurationPattern.findFirstIn(_).isDefined)
      .findFirst()

    if (maybeStringWithDuration.isPresent) {

      for (matcher <- DurationPattern.findFirstMatchIn(maybeStringWithDuration.get()))
        yield matcher.group(1).toInt * 360 + matcher.group(2).toInt * 60 + matcher.group(3).toInt

    } else {
      None
    }
  }
}
