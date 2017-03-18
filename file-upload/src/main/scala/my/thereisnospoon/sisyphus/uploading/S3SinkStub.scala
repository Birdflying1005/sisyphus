package my.thereisnospoon.sisyphus.uploading

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.util.ByteString

import scala.concurrent.{Future, Promise}
import scala.util.Success

class S3SinkStub extends GraphStageWithMaterializedValue[SinkShape[ByteString], Future[IOResult]] {

  val in: Inlet[ByteString] = Inlet[ByteString]("S3SinkStub.in")
  val shape: SinkShape[ByteString] = SinkShape.of(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[IOResult]) = {

    val promise = Promise[IOResult]

    val logic = new GraphStageLogic(shape) {

      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          grab(in)
          pull(in)
        }
        override def onUpstreamFinish(): Unit = promise.success(IOResult(0, Success(Done)))
      })
    }

    (logic, promise.future)
  }
}
