package my.thereisnospoon.sisyphus.streaming

import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.model.headers.{ByteRange, Range, RangeUnits}

class HttpEndpointTest extends FlatSpec with Matchers {

  "extractRange PF " should "be able to extract bytes range from header" in {

    val byteRange = ByteRange(0, 100)
    val rangeHeader = Range(RangeUnits.Bytes,  byteRange :: Nil)
    val extractedByteRange = HttpEndpoint.extractRange(rangeHeader)
    extractedByteRange.get should equal(byteRange)
  }
}
