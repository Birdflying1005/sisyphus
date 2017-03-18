package my.thereisnospoon.sisyphus.uploading.processing

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, Props}

class VideoProcessingActor(videoProcessingService: VideoProcessingService) extends Actor with ActorLogging{

  import VideoProcessingActor._

  override def receive: Receive = {
    case ProcessVideo(path) =>

      val processingResult =
        for (thumbnailPath <- videoProcessingService.generateThumbnail(path);
           duration <- videoProcessingService.calculateDuration(path))
          yield (thumbnailPath, duration)

      processingResult match {
        case Some((thumbPath, duration)) =>
          sender() ! ProcessingResult(thumbPath, duration)

        case None => sender() ! VideoProcessingError
      }
  }
}

object VideoProcessingActor {

  case class ProcessVideo(path: Path)
  case class ProcessingResult(pathToThumbnail: Path, duration: Int)
  case object VideoProcessingError

  def props(videoProcessingService: VideoProcessingService): Props =
    Props(classOf[VideoProcessingActor], videoProcessingService)
}
