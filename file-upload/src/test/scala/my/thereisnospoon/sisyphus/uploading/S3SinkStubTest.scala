package my.thereisnospoon.sisyphus.uploading

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

class S3SinkStubTest extends TestKit(ActorSystem("test-system")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val materializer = ActorMaterializer()

  "S3SinkStub" should "consume all inbound stream and successfully complete Future" in {

    val s3SinkStub: Sink[ByteString, Future[IOResult]] = Sink.fromGraph(new S3SinkStub)
    val source: Source[ByteString, _] = Source.single(ByteString.fromArray(new Array[Byte](100)))
    val result: Future[IOResult] = source.runWith(s3SinkStub)

    val IOResult(_, tr: Try[Done]) = Await.result(result, 1.second)
    tr shouldEqual Success(Done)
  }
}
