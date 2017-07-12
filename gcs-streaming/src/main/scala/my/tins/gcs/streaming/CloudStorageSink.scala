package my.tins.gcs.streaming

import akka.Done
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{ActorAttributes, Attributes, Inlet, SinkShape}
import akka.util.ByteString
import com.google.cloud.WriteChannel
import com.google.cloud.storage.{BlobInfo, Storage}
import com.typesafe.scalalogging.Logger

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class CloudStorageSink(blobInfo: BlobInfo, storageService: Storage)
    extends GraphStageWithMaterializedValue[SinkShape[ByteString], Future[Done]] {

  private val log = Logger(classOf[CloudStorageSink])

  val in: Inlet[ByteString] = Inlet("CloudStorageSink")

  override def shape: SinkShape[ByteString] = SinkShape(in)

  override def initialAttributes: Attributes =
    ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher")

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {

    val promise = Promise[Done]
    val logic = new GraphStageLogic(shape) {

      private var writeChannel: WriteChannel = _

      private def completeStageWithError(ex: Throwable) = {
        completeStage()
        promise.tryFailure(ex)
      }

      override def preStart(): Unit = {

        val ioResult= for {
          blob <- Try(storageService.create(blobInfo))
          writer <- Try(blob.writer())
        } yield writer

        ioResult match {
          case Success(w) =>
            writeChannel = w
            log.debug("Pulling in")
            pull(in)
          case Failure(ex) =>
            completeStageWithError(ex)
        }
      }

      override def postStop(): Unit = {
        log.debug("Closing the channel")
        Try(
          if (writeChannel != null) writeChannel.close()
        ) match {
          case Failure(ex) =>
            log.error("Error during channel closing", ex)
          case Success(_) =>
        }
      }

      setHandler(
        in,
        new InHandler {

          override def onPush(): Unit = {

            val chunk = grab(in)
            val bytesToWrite = chunk.size
            val byteBuffer = chunk.asByteBuffer

            log.debug(s"Writing $bytesToWrite bytes")

            @tailrec
            def write(remainedBytes: Int): Unit =
              if (remainedBytes > 0)
                write(remainedBytes - writeChannel.write(byteBuffer))

            Try(write(bytesToWrite)) match {
              case Success(_) =>
                log.debug("Bytes have been written")
                pull(in)
              case Failure(ex) =>
                completeStageWithError(ex)
            }
          }

          override def onUpstreamFinish(): Unit = promise.success(Done)

          override def onUpstreamFailure(ex: Throwable): Unit =
            promise.failure(ex)
        }
      )
    }

    (logic, promise.future)
  }
}
