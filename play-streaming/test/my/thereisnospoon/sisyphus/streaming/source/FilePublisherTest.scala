package my.thereisnospoon.sisyphus.streaming.source

import java.io.{File, FileOutputStream}
import java.nio.file.Paths
import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class FilePublisherTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  implicit var actorSystem: ActorSystem = _
  implicit var materializer: ActorMaterializer = _

  var data: Array[Byte] = _
  var fileWithData: File = _

  override def beforeAll(): Unit = {

    actorSystem = ActorSystem("test-system")
    materializer = ActorMaterializer()

    fileWithData = File.createTempFile("fp_test", ".data")
    val outputStream = new FileOutputStream(fileWithData)
    val fileLength = 500000
    data = new Array[Byte](fileLength)
    Random.nextBytes(data)
    outputStream.write(data)
    outputStream.close()
  }

  override def afterAll(): Unit = {

    materializer.shutdown()
    actorSystem.terminate()

    fileWithData.delete()
  }

  "FilePublisher" should "read and publish whole file when supplied range includes whole file" in {

    val pathToFile = Paths.get(fileWithData.getAbsolutePath)
    val fileSource = Source.actorPublisher(FilePublisher.props(pathToFile, 0, data.length))
    val future = fileSource.runWith(Sink.seq)

    val readData = Await.result(future, 50.seconds)
    readData should be eq data.toSeq
  }

  it should "read and publish chunk of the file as per supplied bytes range" in {

    val start = Random.nextInt(data.length / 3)
    val end = Random.nextInt(data.length / 3) + data.length / 3

    val sliceOfData: Array[Byte] = data.slice(start, end + 1)

    val pathToFile = Paths.get(fileWithData.getAbsolutePath)
    val fileSource: Source[ByteString, _] = Source.actorPublisher(FilePublisher.props(pathToFile, start, end))
    val future = fileSource.runWith(Sink.seq)

    val readData: Seq[ByteString] = Await.result(future, 3.seconds)
    val d: Array[Byte] = readData.view.flatMap(_.toList).toArray
    println(d.length)
  }
}
