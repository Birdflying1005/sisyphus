package my.thereisnospoon.sisyphus.streaming.source

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy

import scala.concurrent.Future

@ImplementedBy(classOf[LocalSourceProvider])
trait SourceProvider {

  def source(fileId: String, range: Range): Source[ByteString, Future[IOResult]]

  def getFileLength(fileId: String): Future[Long]
}
