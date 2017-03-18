package my.thereisnospoon.sisyphus.uploading

import java.nio.file.Paths

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.DuplicationCheckServiceComponent
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class UploadRouteTest extends FlatSpec with ScalatestRouteTest with Matchers {

  val appContext = new ActorSystemComponent with Configuration with DuplicationCheckServiceComponent
    with VideoProcessingComponent with UploadRouteComponent

  val testVideoPath = Paths.get("../test.webm")

  "/upload endpoint" should "receive file and return `Done` response" in {

    val videoData = Await.result(FileIO.fromPath(testVideoPath).runWith(Sink.fold(ByteString()) {_ ++ _}), 1.second)

    val multipartForm = Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "file",
      HttpEntity(ContentTypes.`application/octet-stream`, videoData),
      Map("filename" -> "some.webm")
    ))

    Post("/upload", multipartForm) ~> appContext.uploadRoute.route ~> check {
      status shouldEqual StatusCodes.OK
    }
  }
}
