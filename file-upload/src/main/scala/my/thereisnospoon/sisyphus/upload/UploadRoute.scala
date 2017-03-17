package my.thereisnospoon.sisyphus.upload

import java.nio.file.{Path, Paths}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream._
import akka.stream.scaladsl.{Broadcast, FileIO, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.scaladsl.GraphDSL.Implicits._

import scala.concurrent.Future
import akka.util.ByteString

class UploadRoute(tempFilesFolder: String) {

  val route: Route = post {
    path("upload") {

      extractRequestContext { ctx =>
        implicit val materializer = ctx.materializer

        fileUpload("file") {
          case (_, byteSource) =>

            val tempFileName = java.util.UUID.randomUUID().toString
            val tempFilePath = Paths.get(tempFilesFolder, tempFileName)

            val graph: RunnableGraph[(Future[IOResult], Future[IOResult])] =
              uploadProcessingGraph(byteSource, tempFilePath)

            val (localIO, persistentStorageIO) = graph.run()

            complete("")
        }
      }
    }
  }

  private def uploadProcessingGraph(byteSource: Source[ByteString, Any],
                                    tempFilePath: Path): RunnableGraph[(Future[IOResult], Future[IOResult])] = {

    val localFileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(tempFilePath)
    val persistentStorageSink: Sink[ByteString, Future[IOResult]] = Sink.ignore

    RunnableGraph.fromGraph(GraphDSL.create(
      localFileSink,
      persistentStorageSink)((_, _)) { implicit builder => (lfSink, psSink) =>

      val uploadingFile: Outlet[ByteString] = builder.add(byteSource).out
      val fanOut: UniformFanOutShape[ByteString, ByteString] = builder.add(Broadcast[ByteString](2))

      uploadingFile ~> fanOut ~> lfSink
                       fanOut ~> psSink

      ClosedShape
    })
  }
}
