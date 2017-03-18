package my.thereisnospoon.sisyphus.uploading

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait ActorSystemComponent {

  implicit lazy val system: ActorSystem = ActorSystem("upload-system")
  implicit lazy val materializer = ActorMaterializer()
}
