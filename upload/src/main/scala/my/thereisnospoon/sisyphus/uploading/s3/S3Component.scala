package my.thereisnospoon.sisyphus.uploading.s3

import akka.stream.alpakka.s3.auth.AWSCredentials
import akka.stream.alpakka.s3.scaladsl.S3Client
import my.thereisnospoon.sisyphus.uploading.{ActorSystemComponent, Configuration}

trait S3Component {this: Configuration with ActorSystemComponent =>

  private lazy val s3Client: S3Client = {
    val awsCredentials = AWSCredentials(accessKeyId = "my-AWS-access-key-ID", secretAccessKey = "my-AWS-password")
    new S3Client(credentials = awsCredentials, region = "")(actorSystem, actorMaterializer)
  }

  lazy val s3SinkProvider: S3SinkProvider = {
    val bucket = config.getString("sisyphus.upload.s3.bucket")
    new RealS3SinkProvider(bucket, s3Client)
  }
}
