package my.thereisnospoon.sisyphus.streaming.source.local
import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import my.thereisnospoon.sisyphus.streaming.source.SourceProvider

import scala.concurrent.{ExecutionContext, Future}

class LocalSourceProvider(filesFolderPath: String, system: ActorSystem) extends SourceProvider {

  implicit val ioExecutionContext: ExecutionContext =
    system.dispatchers.lookup("sisyphus.streaming.blocking-io-dispatcher")

  override def source(fileId: String, range: (Long, Long)): Source[ByteString, _] = {

    val (start, end) = range
    Source.actorPublisher[ByteString](FilePublisher.props(Paths.get(filesFolderPath, fileId), start, end))
  }

  override def getFileLength(fileId: String): Future[Long] =
    Future {
      new File(filesFolderPath, fileId).length()
    }
}
