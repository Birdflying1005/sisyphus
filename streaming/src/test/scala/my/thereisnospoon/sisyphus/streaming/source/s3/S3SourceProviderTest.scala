package my.thereisnospoon.sisyphus.streaming.source.s3

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Random

class S3SourceProviderTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  override def afterAll(): Unit = wireMockServer.stop()

  val wireMockServer = {
    val server = new WireMockServer(
      wireMockConfig()
        .dynamicPort()
    )
    server.start()
    server
  }

  val wireMock = new WireMock("localhost", wireMockServer.port())

  val bucket = "bkt"

  val configForMock =
    s"""
      |sisyphus.streaming {
      |  server {
      |    host = "0.0.0.0"
      |    port = 8484
      |    secure = false
      |  }
      |
      |  s3 {
      |    access-key-id = ""
      |    secret-access-key = ""
      |    region = ""
      |    bucket = $bucket
      |  }
      |}
      |
      |akka.stream.alpakka.s3 {
      |  buffer = "memory"
      |
      |  proxy {
      |    host = "localhost"
      |    port = ${wireMockServer.port()}
      |    secure = false
      |  }
      |
      |  path-style-access = true
      |}
    """.stripMargin

  implicit val system = ActorSystem("S3SourceProviderTest", ConfigFactory.parseString(configForMock)
    .withFallback(ConfigFactory.load()))

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val providerToMock = new S3SourceProvider(ConfigFactory.parseString(configForMock))

  val fileId = "f1"
  val fileLength = 100L

  "S3SourceProvider" should "construct correct bucket uri when using against S3 mock" in {
    providerToMock.bucketUri shouldEqual s"http://localhost:${wireMockServer.port()}/bkt"
  }

  it should "construct correct bucket uri when using against real S3 bucket" in {

    val configForRealS3 =
      """
        |sisyphus.streaming {
        |  server {
        |    host = "0.0.0.0"
        |    port = 8484
        |    secure = false
        |  }
        |
        |  s3 {
        |    access-key-id = ""
        |    secret-access-key = ""
        |    region = "eu-west-1"
        |    bucket = "bkt"
        |  }
        |}
        |
        |akka.stream.alpakka.s3 {
        |  buffer = "memory"
        |
        |  proxy {
        |    host = ""
        |    port = 8383
        |    secure = true
        |  }
        |
        |  path-style-access = false
        |}
      """.stripMargin

    val config = ConfigFactory.parseString(configForRealS3)
    val provider = new S3SourceProvider(config)
    provider.bucketUri shouldEqual "https://bkt.s3-eu-west-1.amazonaws.com"
  }

  it should "retrieve file length" in {

    wireMock.register(
      patch(urlEqualTo(s"/$bucket/$fileId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("ContentLength", fileLength.toString))
    )

    providerToMock.getFileLength(fileId).futureValue shouldEqual fileLength
  }

  it should "retrieve given range of file data" in {

    val fileData = new Array[Byte](fileLength.toInt)
    Random.nextBytes(fileData)

    val (start, end) = (1, 15)

    val rangeOfData = fileData.slice(start, end + 1)

    wireMock.register(
      get(urlEqualTo(s"/$bucket/$fileId"))
        .withHeader("Range", new EqualToPattern(s"bytes=$start-$end"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(rangeOfData)
        )
    )

    val returnedDataFuture = providerToMock.source(fileId, (start, end)).runReduce(_ ++ _).map(_.toArray)
    returnedDataFuture.futureValue shouldEqual rangeOfData
  }
}
