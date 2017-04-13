package my.thereisnospoon.sisyphus.uploading

import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.{DuplicationCheckService, DuplicationCheckServiceComponent}
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingActor.{ProcessVideo, VideoProcessingError}
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingComponent
import my.thereisnospoon.sisyphus.uploading.s3.{S3Component, S3SinkProvider, S3SinkStub}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class UploadRouteTest
  extends FlatSpec
    with ScalatestRouteTest
    with Matchers
    with OneInstancePerTest
    with MockitoSugar {

  val tempFolder = Files.createTempDirectory("route_test")
  val testVideoPath = Paths.get("../test.webm")
  val videoData = Await.result(FileIO.fromPath(testVideoPath).runWith(Sink.fold(ByteString()) {
    _ ++ _
  }), 1.second)

  val multipartForm = Multipart.FormData(
    Multipart.FormData.BodyPart.Strict(
      "file",
      HttpEntity(ContentTypes.`application/octet-stream`, videoData),
      Map("filename" -> "some.webm")
    )
  )

  val appContext = new ActorSystemComponent with Configuration with DuplicationCheckServiceComponent
    with VideoProcessingComponent with S3Component with UploadRouteComponent {

    override lazy val actorSystem: ActorSystem = system
    override lazy val actorMaterializer: ActorMaterializer = materializer

    override lazy val config: Config = {

      val config = ConfigFactory.load()
      val testConfig = ConfigFactory.parseMap(Map("sisyphus.upload.temp-files-folder" -> tempFolder.toString).asJava)
      testConfig.withFallback(config)
    }

    override lazy val s3SinkProvider = new S3SinkProvider {

      override def getSinkForVideo(key: String): Sink[ByteString, Future[MultipartUploadResult]] =
        Sink.fromGraph(new S3SinkStub)

      override def getSinkForThumbnail(key: String): Sink[ByteString, Future[MultipartUploadResult]] =
        Sink.fromGraph(new S3SinkStub)
    }
  }

  "/upload endpoint" should "receive file, return 200 response and clean up temp files after processing" in {

    Post("/upload", multipartForm) ~> appContext.uploadRoute.route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Files.list(tempFolder).count() shouldBe 0
  }

  it should "return 500 response code in case of error during saving of uploading file locally" in {

    val erroneousRoute =
      new UploadRoute("/nonexistentFolder",
        appContext.duplicationCheckService,
        appContext.videoProcessingRouter,
        appContext.s3SinkProvider)

    Post("/upload", multipartForm) ~> erroneousRoute.route ~> check {
      status shouldEqual StatusCodes.InternalServerError
    }
  }

  it should "return BadRequest response in case non-uniqueness of uploaded file and cleanup temp file" in {

    val duplicationCheckService = new DuplicationCheckService {
      override def doesAlreadyExist(fileHash: String): Future[Boolean] =
        Future.successful(true)
    }

    val erroneousRoute =
      new UploadRoute(tempFolder.toString,
        duplicationCheckService,
        appContext.videoProcessingRouter,
        appContext.s3SinkProvider)

    Post("/upload", multipartForm) ~> erroneousRoute.route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }

    Files.list(tempFolder).count() shouldBe 0
  }

  it should "return 500 response code in case of error during video processing and cleanup temp file" in {

    val videoProcessorMockProps = Props(new Actor {
      def receive = {
        case ProcessVideo(_) => sender() ! VideoProcessingError
      }
    })

    val erroneousRoute =
      new UploadRoute(tempFolder.toString,
        appContext.duplicationCheckService,
        system.actorOf(videoProcessorMockProps),
        appContext.s3SinkProvider)

    Post("/upload", multipartForm) ~> erroneousRoute.route ~> check {
      status shouldEqual StatusCodes.InternalServerError
    }

    Files.list(tempFolder).count() shouldBe 0
  }
}
