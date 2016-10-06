package my.thereisnospoon.sisyphus.streaming

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.util.ByteString
import org.bson.types.Binary

class GridFsSource(private val fileId: String) extends GraphStage[SourceShape[ByteString]] {

  val out: Outlet[ByteString] = Outlet("GridFsSource")

  override val shape: SourceShape[ByteString] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    val chunks = GridFsClient.retrieveChunksForFile(fileId).iterator()

    setHandler(out, new OutHandler {

      override def onPull(): Unit = {
        if (chunks.hasNext) {
          val chunkData = chunks.next().get("data", classOf[Binary]).getData
          push(out, ByteString(chunkData))
        } else {
          chunks.close()
          completeStage()
        }
      }
    })
  }
}
