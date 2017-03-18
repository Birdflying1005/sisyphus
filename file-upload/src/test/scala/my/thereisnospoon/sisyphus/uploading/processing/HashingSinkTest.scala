package my.thereisnospoon.sisyphus.uploading.processing

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.apache.commons.codec.digest.DigestUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class HashingSinkTest extends TestKit(ActorSystem("HashingSink-test")) with Matchers with FlatSpecLike
  with BeforeAndAfterAll {

  implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = shutdown(system)

  "HashingSink" should "correctly calculate hash of an input stream" in {

    val data: List[Array[Byte]] = List.fill(10) {

      val array = new Array[Byte](100)
      Random.nextBytes(array)
      array
    }

    val dataAsByteArray: Array[Byte] = data.reduce {_ ++ _}
    val dataAsByteStrings = data.map(ByteString.fromArray)

    val hashFuture: Future[String] = Source.fromIterator(() => dataAsByteStrings.iterator)
      .runWith(Sink.fromGraph(new HashingSink))

    val hash = Await.result(hashFuture, 1.second)
    hash shouldEqual DigestUtils.md5Hex(dataAsByteArray)
  }
}
