package my.thereisnospoon.sisyphus.uploading.processing.dupchek

import scala.concurrent.Future

trait DuplicationCheckService {
  def doesAlreadyExist(fileHash: String): Future[Boolean]
}
