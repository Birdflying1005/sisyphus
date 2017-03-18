package my.thereisnospoon.sisyphus.uploading

import java.nio.file.{Path, Paths}

import akka.pattern.ask
import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, FileIO, GraphDSL, RunnableGraph, Sink, Source}
import akka.util.{ByteString, Timeout}
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.{DuplicationCheckService, HashingSink}
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingActor.{ProcessVideo, ProcessingResult, VideoProcessingError}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UploadRoute(
                   tempFilesFolder: String,
                   duplicationCheckService: DuplicationCheckService,
                   videoProcessor: ActorRef) {

  implicit val timeout: Timeout = 5.seconds

  val route: Route = post {
    path("upload") {

      extractRequestContext { ctx =>
        implicit val materializer = ctx.materializer
        implicit val executionContext = ctx.executionContext

        fileUpload("file") {
          case (_, byteSource) =>

            val tempFileName = java.util.UUID.randomUUID().toString
            val tempFilePath = Paths.get(tempFilesFolder, tempFileName)

            val graph: RunnableGraph[(Future[IOResult], Future[IOResult], Future[String])] =
              uploadProcessingGraph(byteSource, tempFilePath)

            val (localIO, persistentStorageIO, hashFuture) = graph.run()

            onSuccess(hashFuture.flatMap(duplicationCheckService.doesAlreadyExist)) {(videoExists: Boolean) =>

              if (videoExists) {
                complete(StatusCodes.BadRequest, "Video already exists")
              } else {
                onSuccess(mapIOResultFuture(localIO).flatMap(_ => videoProcessor ? ProcessVideo(tempFilePath))) {

                  case VideoProcessingError =>
                    complete(StatusCodes.InternalServerError, "Error during video processing")

                  case ProcessingResult(thumbnailPath, duration) =>


                    //ToDo: Send thumbnail to S3 and metadata to kafka
                    onSuccess(mapIOResultFuture(persistentStorageIO)) {_ =>
                      complete(StatusCodes.OK)
                      //ToDo: Add clean up and error handling
                    }
                }
              }
            }
        }
      }
    }
  }

  private def uploadProcessingGraph(
                                     byteSource: Source[ByteString, Any],
                                     tempFilePath: Path
                                   ): RunnableGraph[(Future[IOResult], Future[IOResult], Future[String])] = {

    val localFileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(tempFilePath)
    val persistentStorageSink: Sink[ByteString, Future[IOResult]] = Sink.fromGraph(new S3SinkStub)
    val hashingSink: Sink[ByteString, Future[String]] = Sink.fromGraph(new HashingSink)

    RunnableGraph.fromGraph(GraphDSL.create(
      localFileSink,
      persistentStorageSink,
      hashingSink)((_, _, _)) { implicit builder => (lfSink, psSink, hashSink) =>

      val uploadingFile: Outlet[ByteString] = builder.add(byteSource).out
      val fanOut: UniformFanOutShape[ByteString, ByteString] = builder.add(Broadcast[ByteString](3))

      uploadingFile ~> fanOut ~> lfSink
                       fanOut ~> psSink
                       fanOut ~> hashSink

      ClosedShape
    })
  }

  private def mapIOResultFuture(ioResult: Future[IOResult])(implicit ec: ExecutionContext): Future[Done] = {

    ioResult.flatMap {
      case IOResult(_, Success(_)) => Future.successful(Done)
      case IOResult(_, Failure(ex)) => Future.failed(ex)
    }
  }
}
