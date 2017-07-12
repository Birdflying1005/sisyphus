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

class CloudStorageSink(blobInfo: BlobInfo, storageService: Storage)
    extends GraphStageWithMaterializedValue[SinkShape[ByteString], Future[Done]] {

  private val log = Logger(classOf[CloudStorageSink])

  val in: Inlet[ByteString] = Inlet("CloudStorageSink")

  override def shape: SinkShape[ByteString] = SinkShape(in)

  override def initialAttributes: Attributes = ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher")

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {

    val promise = Promise[Done]
    val logic = new GraphStageLogic(shape) {

      private var writeChannel: WriteChannel = _

      override def preStart(): Unit = {
        val blob = storageService.create(blobInfo)
        writeChannel = blob.writer()

        log.debug("Pulling in")

        pull(in)
      }

      override def postStop(): Unit = {
        log.debug("Closing the channel")
        writeChannel.close()
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

            write(bytesToWrite)

            log.debug("Bytes have been written")

            pull(in)
          }

          override def onUpstreamFinish(): Unit = promise.success(Done)

          override def onUpstreamFailure(ex: Throwable): Unit = promise.failure(ex)
        }
      )
    }

    (logic, promise.future)
  }
}
