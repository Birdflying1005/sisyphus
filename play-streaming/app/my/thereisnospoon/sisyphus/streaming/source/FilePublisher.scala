package my.thereisnospoon.sisyphus.streaming.source

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Path, StandardOpenOption}

import akka.actor.{ActorLogging, DeadLetterSuppression, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.ByteString

import scala.annotation.tailrec
import scala.util.control.NonFatal

class FilePublisher(pathToFile: Path, startByte: Long, endByte: Long) extends ActorPublisher[ByteString]
  with ActorLogging{

  import FilePublisher._

  private val chunksToBuffer = 10
  private var bytesLeftToRead = endByte - startByte + 1
  private var fileChannel: FileChannel = _
  private val buffer = ByteBuffer.allocate(8096)

  private var bufferedChunks: Vector[ByteString] = _

  override def preStart(): Unit = {
    try {
      fileChannel = FileChannel.open(pathToFile, StandardOpenOption.READ)
      fileChannel.position(startByte)
      bufferedChunks = readAhead(Vector.empty)
    } catch {
      case NonFatal(ex) =>
        log.error(ex, "Got error reading data from file channel")
        onErrorThenStop(ex)
    }
  }

  override def postStop(): Unit = {

    if (fileChannel ne null)
      try fileChannel.close() catch {
        case NonFatal(ex) => log.error(ex, "Error during file channel close")
    }
  }

  override def receive: Receive = {
    case Request(_) =>
      readAndSignalNext()
    case Continue =>
      readAndSignalNext()
    case Cancel =>
      context.stop(self)
  }

  private def readAndSignalNext() = {

    if (isActive) {
      bufferedChunks = readAhead(signalOnNext(bufferedChunks))
      if (isActive && totalDemand > 0) self ! Continue
    }
  }

  @tailrec
  private def signalOnNext(chunks: Vector[ByteString]): Vector[ByteString] = {

    if (chunks.nonEmpty && totalDemand > 0) {
      onNext(chunks.head)
      signalOnNext(chunks.tail)
    } else {
      if (chunks.isEmpty && bytesLeftToRead <= 0) {
        onCompleteThenStop()
      }
      chunks
    }
  }

  @tailrec
  private def readAhead(currentlyBufferedChunks: Vector[ByteString]): Vector[ByteString] = {

    if (currentlyBufferedChunks.size < chunksToBuffer && bytesLeftToRead > 0) {

      val bytesRead = readDataFromChannel()
      bytesRead match {
        case Int.MinValue => Vector.empty
        case -1 =>
          currentlyBufferedChunks // EOF reached
        case _ =>
          buffer.flip()
          val chunk = ByteString(buffer)
          buffer.clear()

          bytesLeftToRead -= bytesRead
          if (bytesLeftToRead < 0) {
            val trimmedChunk = chunk.dropRight(math.abs(bytesLeftToRead.toInt))
            currentlyBufferedChunks :+ trimmedChunk
          } else {
            readAhead(currentlyBufferedChunks :+ chunk)
          }
      }

    } else {
      currentlyBufferedChunks
    }
  }

  private def readDataFromChannel(): Int = {
    try {
      fileChannel.read(buffer)
    } catch {
      case NonFatal(ex) =>
        log.error(ex, "Got error reading data from file channel")
        onErrorThenStop(ex)
        Int.MinValue
    }
  }
}

object FilePublisher {

  private case object Continue extends DeadLetterSuppression

  def props(path: Path, startByte: Long, endByte: Long): Props = Props(classOf[FilePublisher], path, startByte, endByte)
    .withDispatcher("sisyphus.streaming.blocking-io-dispatcher")
}