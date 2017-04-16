package my.thereisnospoon.sisyphus.uploading.processing.dupchek

import scala.concurrent.Future

trait DuplicationCheckServiceComponent {

  lazy val duplicationCheckService = new DuplicationCheckService {
    override def doesAlreadyExist(fileHash: String) = Future.successful(false)
  }
}
