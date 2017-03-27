package my.thereisnospoon.sisyphus.uploading.s3

import akka.Done
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue}
import akka.stream.{Attributes, Inlet, OverflowStrategy, SinkShape}
import akka.util.ByteString

import scala.concurrent.{Future, Promise}

class S3Sink(contentLength: Long, bucket: String)
  extends GraphStageWithMaterializedValue[ByteString, Future[Done]] {

  val in: Inlet[ByteString] = Inlet("S3Sink.in")
  val shape: SinkShape[ByteString] = SinkShape.of(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {

    val promise: Promise[Done] = Promise()
    val source = Source.queue[ByteString](0, OverflowStrategy.backpressure)
    val flow = Http().cachedHostConnectionPool("", 8001)

    val graphStageLogic = new GraphStageLogic(shape) {

    }

    (graphStageLogic, promise.future)
  }
}
