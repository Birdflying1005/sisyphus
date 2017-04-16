package my.thereisnospoon.sisyphus.streaming.source

import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

trait SourceProvider {

  def source(fileId: String, range: (Long, Long)): Source[ByteString, _]

  def getFileLength(fileId: String): Future[Long]
}
