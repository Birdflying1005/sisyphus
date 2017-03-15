package my.thereisnospoon.sisyphus.streaming.controllers

import my.thereisnospoon.sisyphus.streaming.source.SourceProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import play.api.test.FakeRequest

class StreamingControllerTest extends FlatSpec with MockFactory with BeforeAndAfterAll {

  val fileId = "file"

  var sourceProvider: SourceProvider = _
  var testedController: StreamingController = _

  override def beforeAll(): Unit = {
    sourceProvider = mock[SourceProvider]
    sourceProvider expects
    testedController = new StreamingController(sourceProvider)
  }

  "StreamingController" should "return whole file in response when no `Range` header in request" in {
    testedController.streamFile(fileId).apply(FakeRequest())
  }
}
