package my.thereisnospoon.sisyphus.uploading.processing.video

import java.io.IOException

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{ActorRef, OneForOneStrategy}
import akka.routing.FromConfig
import my.thereisnospoon.sisyphus.uploading.{ActorSystemComponent, Configuration}

trait VideoProcessingComponent {
  this: Configuration with ActorSystemComponent =>

  private lazy val videoProcessingService: VideoProcessingService = {

    val uploadConfig = config.getConfig("sisyphus.upload")
    new VideoProcessingService(
      uploadConfig.getString("tempFilesFolder"),
      uploadConfig.getString("ffmpegPath"),
      uploadConfig.getString("ffprobePath")
    )
  }

  lazy val videoProcessingRouter: ActorRef = {

    val routerSupervisionStrategy = OneForOneStrategy() {
      case _: IOException => Resume
    }

    system.actorOf(FromConfig(supervisorStrategy = routerSupervisionStrategy).props(
      VideoProcessingActor.props(videoProcessingService)), "videoProcessingRouter")
  }
}
