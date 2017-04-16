package my.thereisnospoon.sisyphus.streaming

import akka.http.scaladsl.model.{ContentRange, StatusCodes}
import akka.http.scaladsl.model.headers.ByteRange.{FromOffset, Slice}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akka.util.ByteString
import my.thereisnospoon.sisyphus.streaming.source.SourceProvider
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class StreamingServerTest extends FlatSpec with ScalatestRouteTest with Matchers {

  val fileId = "file"
  val fileLength = 100L
  val getUrl = s"/file/$fileId"

  val sourceProvider = {
    val provider = mock(classOf[SourceProvider])
    when(provider.getFileLength(any())).thenReturn(Future.successful(fileLength))
    val emptySource: Source[ByteString, Any] = Source.empty[ByteString]
    when(provider.source(anyString(), any())).thenReturn(emptySource)
    provider
  }

  val streamingRoute = new StreamingServer(sourceProvider).route

  "streaming server" should "return whole file in response when no `Range` header in request" in {

    Get(getUrl) ~> streamingRoute ~> check {
      headers.find(_.is("content-range")).get shouldEqual `Content-Range`(ContentRange(0, fileLength - 1, fileLength))
    }
  }

  it should "return remaining range of file for range such as 'Range bytes=2-'" in {

    Get(getUrl).withHeaders(Range(FromOffset(2))) ~> streamingRoute ~> check {
      headers.find(_.is("content-range")).get shouldEqual `Content-Range`(ContentRange(2, fileLength - 1, fileLength))
    }
  }

  it should "return exact range of file when exact range is specified in request" in {

    val (start, end) = (5, 30)
    Get(getUrl).withHeaders(Range(Slice(start, end))) ~> streamingRoute ~> check {
      headers.find(_.is("content-range")).get shouldEqual `Content-Range`(ContentRange(start, end, fileLength))
    }
  }

  it should "return response with Partial content status and with 'Accept-Ranges' header" in {

    Get(getUrl) ~> streamingRoute ~> check {
      headers.find(_.is("accept-ranges")).get shouldEqual `Accept-Ranges`(RangeUnits.Bytes)
      status shouldEqual StatusCodes.PartialContent
    }
  }
}
