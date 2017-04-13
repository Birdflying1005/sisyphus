package my.thereisnospoon.sisyphus.uploading.s3

import akka.http.scaladsl.model.Uri
import akka.stream._
import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.util.ByteString

import scala.concurrent.{Future, Promise}

class S3SinkStub extends GraphStageWithMaterializedValue[SinkShape[ByteString], Future[MultipartUploadResult]] {

  val in: Inlet[ByteString] = Inlet[ByteString]("S3SinkStub.in")
  val shape: SinkShape[ByteString] = SinkShape.of(in)

  type StageLogicAndMaterialization = (GraphStageLogic, Future[MultipartUploadResult])

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): StageLogicAndMaterialization  = {

    val promise = Promise[MultipartUploadResult]

    val logic = new GraphStageLogic(shape) {

      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          grab(in)
          pull(in)
        }
        override def onUpstreamFinish(): Unit = promise.success(MultipartUploadResult(Uri(""), "", "", ""))
      })
    }

    (logic, promise.future)
  }
}
