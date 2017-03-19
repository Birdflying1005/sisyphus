package my.thereisnospoon.sisyphus.uploading

import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.DuplicationCheckServiceComponent
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class UploadRouteTest extends FlatSpec with ScalatestRouteTest with Matchers with OneInstancePerTest {

  val tempFolder: Path = Files.createTempDirectory("route_test")

  val appContext = new ActorSystemComponent with Configuration with DuplicationCheckServiceComponent
    with VideoProcessingComponent with UploadRouteComponent {

    override lazy val actorSystem: ActorSystem = system
    override lazy val actorMaterializer: ActorMaterializer = materializer

    override lazy val config: Config = {

      val config = ConfigFactory.load()
      val testConfig = ConfigFactory.parseMap(Map("sisyphus.upload.tempFilesFolder" -> tempFolder.toString).asJava)
      testConfig.withFallback(config)
    }
  }

  val testVideoPath = Paths.get("../test.webm")

  "/upload endpoint" should "receive file, return 200 response and clean up temp files after processing" in {

    val videoData = Await.result(FileIO.fromPath(testVideoPath).runWith(Sink.fold(ByteString()) {_ ++ _}), 1.second)

    val multipartForm = Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "file",
      HttpEntity(ContentTypes.`application/octet-stream`, videoData),
      Map("filename" -> "some.webm")
    ))

    Post("/upload", multipartForm) ~> appContext.uploadRoute.route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Files.list(tempFolder).count() shouldBe 0
  }
}
