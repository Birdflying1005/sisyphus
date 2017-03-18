package my.thereisnospoon.sisyphus.uploading

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FlatSpec, Matchers}

class UploadServerTest extends FlatSpec with ScalatestRouteTest with Matchers {

  "/upload endpoint" should "receive file and return `Done` response" in {

    val multipartForm = Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "file",
      HttpEntity(ContentTypes.`application/octet-stream`, new Array[Byte](1000)),
      Map("filename" -> "some.webm")
    ))

    Post("/upload", multipartForm) ~> new UploadRoute("/tmp").route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "Done"
    }
  }
}
