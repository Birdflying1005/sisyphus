package my.thereisnospoon.sisyphus.streaming.source
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.google.inject.Inject
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class LocalSourceProvider @Inject() (configuration: Configuration) extends SourceProvider {

  private val filesFolderPath = configuration.getString("sisyphus.streaming.filesFolderPath").get

  implicit val ioExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def source(fileId: String, range: (Long, Long)): Source[ByteString, Future[IOResult]] = {
    FileIO.fromPath(Paths.get(filesFolderPath, fileId))
  }

  override def getFileLength(fileId: String): Future[Long] = {

    Future {
      new File(filesFolderPath, fileId).length()
    }
  }
}
