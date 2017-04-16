package my.thereisnospoon.sisyphus.uploading.processing.dupchek

import java.security.MessageDigest

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import akka.util.ByteString
import org.apache.commons.codec.binary.Hex

import scala.concurrent.{Future, Promise}

class HashingSink extends GraphStageWithMaterializedValue[SinkShape[ByteString], Future[String]] {

  val in: Inlet[ByteString] = Inlet[ByteString]("HashingSink.in")
  val shape: SinkShape[ByteString] = SinkShape.of(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {

    val promise: Promise[String] = Promise()

    val logic = new GraphStageLogic(shape) {

      val digest = MessageDigest.getInstance("MD5")

      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {

        override def onPush(): Unit = {
          val data: ByteString = grab(in)
          digest.update(data.toArray)
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          val hash = Hex.encodeHexString(digest.digest())
          promise.success(hash)
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          digest.reset()
          promise.failure(ex)
        }
      })
    }

    (logic, promise.future)
  }
}
