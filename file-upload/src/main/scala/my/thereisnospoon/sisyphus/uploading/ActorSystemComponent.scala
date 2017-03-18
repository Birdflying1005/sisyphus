package my.thereisnospoon.sisyphus.uploading

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait ActorSystemComponent {

  implicit val actorSystem: ActorSystem
  implicit val actorMaterializer: ActorMaterializer
}
