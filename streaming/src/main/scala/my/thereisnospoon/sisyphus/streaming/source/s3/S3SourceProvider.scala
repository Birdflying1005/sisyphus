package my.thereisnospoon.sisyphus.streaming.source.s3

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.ByteRange
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.auth.AWSCredentials
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import my.thereisnospoon.sisyphus.streaming.source.SourceProvider

import scala.concurrent.Future

class S3SourceProvider(config: Config)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends SourceProvider with LazyLogging {

  private implicit val executionContext = actorSystem.dispatcher

  private val s3Client = {
    val s3Config = config.getConfig("sisyphus.streaming.s3")
    val credentials =
      AWSCredentials(accessKeyId = s3Config.getString("access-key-id"),
                     secretAccessKey = s3Config.getString("secret-access-key"))

    new S3Client(credentials, s3Config.getString("region"))
  }

  private val bucket = config.getString("sisyphus.streaming.s3.bucket")

  val bucketUri: String = {

    val alpakkaConfig = config.getConfig("akka.stream.alpakka.s3")
    val pathStyleAccess = alpakkaConfig.getBoolean("path-style-access")
    val region = config.getString("sisyphus.streaming.s3.region")

    val proxyProperty = "proxy.host"
    val host =
      if (alpakkaConfig.getIsNull(proxyProperty) || alpakkaConfig.getString(proxyProperty) == "")
        s"s3-$region.amazonaws.com"
      else
        s"${alpakkaConfig.getString(proxyProperty)}:${alpakkaConfig.getInt("proxy.port")}"

    val bucketPath = if (pathStyleAccess) s"$host/$bucket" else s"$bucket.$host"

    if (alpakkaConfig.getBoolean("proxy.secure"))
      s"https://$bucketPath"
    else
      s"http://$bucketPath"

  }

  //ToDo: Needs to replaced with HEAD request and getting `Content-Length` when using real S3
  override def getFileLength(fileId: String): Future[Long] = {

    logger.debug(s"Making request for length to $bucketUri/$fileId")

    for (response <- Http().singleRequest(HttpRequest(HttpMethods.PATCH, s"$bucketUri/$fileId")))
      yield response.headers.find(_.is("contentlength")).get.value.toLong
  }

  override def source(fileId: String, range: (Long, Long)): Source[ByteString, Any] = {
    val (start, end) = range
    s3Client.download(bucket, fileId, ByteRange(start, end))
  }
}
