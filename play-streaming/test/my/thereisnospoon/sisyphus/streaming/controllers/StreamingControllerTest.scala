package my.thereisnospoon.sisyphus.streaming.controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import my.thereisnospoon.sisyphus.streaming.source.SourceProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import play.api.http.{HeaderNames, Status}
import play.api.mvc.{ResponseHeader, Result}
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class StreamingControllerTest extends FlatSpec with MockFactory with BeforeAndAfter with Matchers {

  val fileId = "file"
  val fileLength = 100L

  val timeout = 1.second

  var sourceProvider: SourceProvider = _
  var testedController: StreamingController = _

  before {
    withExpectations {
      sourceProvider = stub[SourceProvider]
      (sourceProvider.getFileLength _).when(*).returns(Future.successful(fileLength))
      (sourceProvider.source _).when(fileId, *).returns(Source.empty[ByteString])

      println(sourceProvider.getFileLength(fileId))

      testedController = new StreamingController(sourceProvider)
    }
  }

  "StreamingController" should "return whole file in response when no `Range` header in request" in {

    val Result(header, _) = Await.result(testedController.streamFile(fileId).apply(FakeRequest()), timeout)
    header.headers(HeaderNames.CONTENT_RANGE) should equal(s"bytes 0-${fileLength - 1}/$fileLength")
  }

  it should "return remaining range of file for range such as 'Range bytes=2-'" in {

    val request = FakeRequest().withHeaders(HeaderNames.RANGE -> "bytes=2-")
    val Result(header, _) = Await.result(testedController.streamFile(fileId).apply(request), timeout)
    header.headers(HeaderNames.CONTENT_RANGE) should equal(s"bytes 2-${fileLength - 1}/$fileLength")
  }

  it should "return exact range of file when exact range is specified in request" in {

    val request = FakeRequest().withHeaders(HeaderNames.RANGE -> "bytes=5-30")
    val Result(header, _) = Await.result(testedController.streamFile(fileId).apply(request), timeout)
    header.headers(HeaderNames.CONTENT_RANGE) should equal(s"bytes 5-30/$fileLength")
  }

  it should "return response with Partial content status and with 'Accept-Ranges' and 'Content-Range' headers" in {

    val Result(ResponseHeader(status, headers, _), _) = Await
      .result(testedController.streamFile(fileId).apply(FakeRequest()), timeout)

    status should equal(Status.PARTIAL_CONTENT)
    headers(HeaderNames.ACCEPT_RANGES) should equal("bytes")
    headers.keySet should contain(HeaderNames.CONTENT_RANGE)
  }
}
