package my.thereisnospoon.sisyphus.uploading.s3

import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.Future

class RealS3SinkProvider(bucket: String, s3Client: S3Client) extends S3SinkProvider {

  def getSinkForS3(key: String): Sink[ByteString, Future[MultipartUploadResult]] =
    s3Client.multipartUpload(bucket, key)
}
