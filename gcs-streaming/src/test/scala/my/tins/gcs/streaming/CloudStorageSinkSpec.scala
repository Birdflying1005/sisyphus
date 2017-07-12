package my.tins.gcs.streaming

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSource
import akka.util.ByteString
import com.google.cloud.WriteChannel
import com.google.cloud.storage.{Blob, BlobInfo, Storage}
import org.mockito.Mockito._
import org.mockito.{Matchers => matchers}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

class CloudStorageSinkSpec extends FlatSpec with MockitoSugar with BeforeAndAfterAll with Matchers {

  implicit var actorSystem: ActorSystem = _
  implicit var materializer: ActorMaterializer = _

  val storageService = mock[Storage]
  val blobInfo = mock[BlobInfo]
  val blob = mock[Blob]
  val channel = mock[WriteChannel]

  when(storageService.create(blobInfo)).thenReturn(blob)
  when(blob.writer()).thenReturn(channel)

  override def beforeAll(): Unit = {

    actorSystem = ActorSystem()
    materializer = ActorMaterializer()
  }

  override def afterAll(): Unit = actorSystem.terminate()

  behavior of "CloudStorageSink"

  it should "successfully consume data and write it to NIO channel" in {

    val totalBytesWritten = new AtomicInteger(0)

    when(channel.write(matchers.any(classOf[ByteBuffer])))
      .thenAnswer(inv => {
        val bytesWritten = inv.getArgumentAt(0, classOf[ByteBuffer]).capacity()
        totalBytesWritten.addAndGet(bytesWritten); bytesWritten
      })

    val (upstream, doneToggle) =
      TestSource.probe[ByteString].toMat(new CloudStorageSink(blobInfo, storageService))(Keep.both).run()

    upstream.sendNext(ByteString(new Array[Byte](10)))
    upstream.sendNext(ByteString(new Array[Byte](9)))
    upstream.sendComplete()

    Await.result(doneToggle, 5.seconds) shouldEqual Done
    verify(channel).close()
    totalBytesWritten.get() shouldEqual 10 + 9
  }

  it should "fail materialized Future in case of upstream error" in {

    val (upstream, doneToggle) =
      TestSource.probe[ByteString].toMat(new CloudStorageSink(blobInfo, storageService))(Keep.both).run()

    upstream.sendError(new RuntimeException)

    val processingResult = Await.ready(doneToggle, 5.seconds).value
    processingResult shouldBe a [Some[_]]
    processingResult.get shouldBe a [Failure[_]]
  }
}
