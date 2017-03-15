package my.thereisnospoon.sisyphus.streaming.source
import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.Inject
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class LocalSourceProvider @Inject() (configuration: Configuration, system: ActorSystem) extends SourceProvider {

  private val filesFolderPath = configuration.getString("sisyphus.streaming.filesFolderPath").get

  implicit val ioExecutionContext: ExecutionContext = system.dispatchers.lookup("sisyphus.streaming.blocking-io-dispatcher")

  override def source(fileId: String, range: (Long, Long)): Source[ByteString, _] = {

    val (start, end) = range
    Source.actorPublisher[ByteString](FilePublisher.props(Paths.get(filesFolderPath, fileId), start, end))
  }

  override def getFileLength(fileId: String): Future[Long] = {

    Future {
      new File(filesFolderPath, fileId).length()
    }
  }
}
