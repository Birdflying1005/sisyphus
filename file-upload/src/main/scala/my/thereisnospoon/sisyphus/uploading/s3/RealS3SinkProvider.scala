package my.thereisnospoon.sisyphus.uploading.s3

import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.Future

class RealS3SinkProvider(
                      videoBucket: String,
                      thumbnailsBucket: String,
                      s3Client: S3Client) extends S3SinkProvider {

  def getSinkForVideo(key: String): Sink[ByteString, Future[MultipartUploadResult]] = getSink(videoBucket, key)

  def getSinkForThumbnail(key: String): Sink[ByteString, Future[MultipartUploadResult]] = getSink(thumbnailsBucket, key)

  private def getSink(bucket: String, key: String) = s3Client.multipartUpload(bucket, key)
}
