package my.thereisnospoon.sisyphus.uploading.s3

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class S3SinkStubTest extends TestKit(ActorSystem("test-system")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val materializer = ActorMaterializer()

  "S3SinkStub" should "consume all inbound stream and successfully complete Future" in {

    val s3SinkStub: Sink[ByteString, Future[MultipartUploadResult]] = Sink.fromGraph(new S3SinkStub)
    val source: Source[ByteString, _] = Source.single(ByteString.fromArray(new Array[Byte](100)))
    val future: Future[MultipartUploadResult] = source.runWith(s3SinkStub)

    Await.result(future, 1.second)
    future.value match {
      case Some(Failure(_)) => fail()
      case _ => succeed
    }
  }
}
