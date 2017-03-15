package my.thereisnospoon.sisyphus.streaming.source

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy

import scala.concurrent.Future

@ImplementedBy(classOf[LocalSourceProvider])
trait SourceProvider {

  def source(fileId: String, range: (Long, Long)): Source[ByteString, _]

  def getFileLength(fileId: String): Future[Long]
}
