package my.thereisnospoon.sisyphus.uploading

import java.nio.file.{Files, Path, Paths}

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.s3.scaladsl.MultipartUploadResult
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}
import akka.util.{ByteString, Timeout}
import com.typesafe.scalalogging.Logger
import my.thereisnospoon.sisyphus.uploading.processing.dupchek.{DuplicationCheckService, HashingSink, NonUniqueVideoException}
import my.thereisnospoon.sisyphus.uploading.processing.video.VideoProcessingActor._
import my.thereisnospoon.sisyphus.uploading.s3.S3SinkProvider

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UploadRoute(
                   tempFilesFolder: String,
                   duplicationCheckService: DuplicationCheckService,
                   videoProcessor: ActorRef,
                   s3SinkProvider: S3SinkProvider) {

  private val log = Logger(classOf[UploadRoute])

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

            val graph: RunnableGraph[(Future[IOResult], Future[MultipartUploadResult], Future[String])] =
              uploadGraph(byteSource, tempFilePath, tempFileName)

            val (localIO, persistentStorageIO, hashFuture) = graph.run()

            val processingSource = processingGraph(
              Source.fromFuture(mapIOResultFuture(localIO).map(_ => tempFilePath)),
              Source.fromFuture(persistentStorageIO),
              Source.fromFuture(hashFuture))

            val processingFuture = processingSource.runWith(Sink.head)
            for (_ <- processingFuture.failed)
              cleanUp(tempFileName)

            onComplete(processingFuture) {
              case Success(_) =>
                cleanUp(tempFileName)
                complete(HttpResponse(entity = HttpEntity(tempFileName)))
              case Failure(ex) => ex match {
                case _: NonUniqueVideoException => complete(StatusCodes.BadRequest, "Video already exists")
                case _ => complete(StatusCodes.InternalServerError)
              }
            }
        }
      }
    }
  }

  private def uploadGraph(byteSource: Source[ByteString, Any],
                          tempFilePath: Path,
                          videoKey: String
                         ): RunnableGraph[(Future[IOResult], Future[MultipartUploadResult], Future[String])] = {

    val localFileSink = FileIO.toPath(tempFilePath)
    val persistentStorageSink = s3SinkProvider.getSinkForVideo(videoKey)
    val hashingSink = Sink.fromGraph(new HashingSink)

    RunnableGraph.fromGraph(GraphDSL.create(
      localFileSink,
      persistentStorageSink,
      hashingSink)((_, _, _)) { implicit builder => (lfSink, psSink, hashSink) =>

      val fanOut = builder.add(Broadcast[ByteString](3))

      byteSource ~> fanOut ~> lfSink
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

  // ToDo: Move to io dispatcher
  private def cleanUp(tempFileName: String) = {

    //ToDo: Add cleanup of S3
    val tempFilePath = Paths.get(tempFilesFolder, tempFileName)
    val thumbnailPath = Paths.get(tempFilePath.toString + ".png")
    Files.deleteIfExists(tempFilePath)
    Files.deleteIfExists(thumbnailPath)
  }

  private def processingGraph(
                               localIO: Source[Path, Any],
                               s3IO: Source[MultipartUploadResult, Any],
                               hash: Source[String, Any]) = {

    Source.fromGraph(GraphDSL.create() { implicit builder =>

      val uniqueCheck = Flow[String].mapAsync(1)(duplicationCheckService.doesAlreadyExist)
      val zip1 = builder.add(Zip[Path, Boolean])

      val processVideo = Flow[(Path, Boolean)].mapAsync(1) {
        case (path, isNonUnique) =>
          if (isNonUnique)
            throw new NonUniqueVideoException
          else
            videoProcessor ? ProcessVideo(path)
      }

      val processingResult = Flow[Any].map {
        case ProcessingResult(_, _) => Done
        case _ => throw new RuntimeException("Couldn't process video")
      }

      val zip2 = builder.add(Zip[Done, Any])

      hash ~> uniqueCheck ~> zip1.in1
                  localIO ~> zip1.in0
                             zip1.out ~> processVideo ~> processingResult ~> zip2.in0
                                                                     s3IO ~> zip2.in1

      SourceShape(zip2.out)
    })
  }
}
