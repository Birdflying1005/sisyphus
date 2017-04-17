package my.thereisnospoon.sisyphus.uploading.s3

import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.Future

trait S3SinkProvider {

  def getSinkForS3(key: String): Sink[ByteString, Future[MultipartUploadResult]]
}
