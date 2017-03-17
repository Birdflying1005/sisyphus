package my.thereisnospoon.sisyphus.video

import java.nio.file.Path
import akka.actor.{Actor, ActorLogging}

class VideoProcessingActor extends Actor with ActorLogging{

  import VideoProcessingActor._

  override def receive: Receive = {
    case ProcessVideo(path) =>

  }
}

object VideoProcessingActor {
  case class ProcessVideo(path: Path)
  case class ProcessingResult(pathToThumbnail: Path, duration: Int)
}
