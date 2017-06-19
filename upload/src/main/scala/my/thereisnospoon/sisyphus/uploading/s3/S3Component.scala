package my.thereisnospoon.sisyphus.uploading.s3

import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.alpakka.s3.auth.AWSCredentials
import akka.stream.alpakka.s3.scaladsl.S3Client
import my.thereisnospoon.sisyphus.uploading.{ActorSystemComponent, Configuration}

trait S3Component {this: Configuration with ActorSystemComponent =>

  val region = config.getString("sisyphus.upload.s3.region")

  private lazy val s3Client: S3Client = {

    val accessKey = config.getString("sisyphus.upload.s3.access-key-id")
    val secretAccessKey = config.getString("sisyphus.upload.s3.secret-access-key")

    val awsCredentials = AWSCredentials(accessKey, secretAccessKey)
    val settings = new S3Settings(MemoryBufferType, "", None, awsCredentials, region, false)
    new S3Client(settings)(actorSystem, actorMaterializer)
  }

  lazy val s3SinkProvider: S3SinkProvider = {
    val bucket = config.getString("sisyphus.upload.s3.bucket")
    new RealS3SinkProvider(bucket, s3Client)
  }

  lazy val bucketUri: String = {
    val alpakkaConfig = config.getConfig("akka.stream.alpakka.s3")
    val pathStyleAccess = alpakkaConfig.getBoolean("path-style-access")
    val bucket = config.getString("sisyphus.upload.s3.bucket")

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
}
