package my.thereisnospoon.sisyphus.streaming.source

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.util.ByteString

class LocalFileSource(start: Long, end: Long) extends GraphStage[SourceShape[ByteString]] {

  val out: Outlet[ByteString] = Outlet("LocalFileSource")

  override val shape: SourceShape[ByteString] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {


  }
}
