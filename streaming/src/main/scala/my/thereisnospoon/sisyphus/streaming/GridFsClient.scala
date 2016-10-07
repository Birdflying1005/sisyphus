package my.thereisnospoon.sisyphus.streaming

import com.mongodb.MongoClient
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoClients
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Sorts._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import org.bson.Document
import org.bson.types.ObjectId

import scala.concurrent.{Future, Promise}

object GridFsClient {

  val mongoUri = "mongodb://localhost:27017"
  val dbName = "webm-haus"

  private val asyncMongoClient = MongoClients.create(mongoUri)
  private val mongoClient = new MongoClient
  private val chunksCollection = mongoClient.getDatabase(dbName).getCollection("fs.chunks")
  private val filesCollection = asyncMongoClient.getDatabase(dbName).getCollection("fs.files")

  def retrieveChunksForFile(fileId: String): FindIterable[Document] =
    chunksCollection.find(Filters.eq("files_id", new ObjectId(fileId))).sort(ascending("n"))

  def retrieveFileSize(fileId: String): Future[Long] = {

    val lengthRetrievePromise = Promise[Long]

    filesCollection.find(Filters.eq("_id", new ObjectId(fileId))).projection(include("length"))
      .first((res: Document, t: Throwable) => {

        Option(res) match {
          case Some(document) => lengthRetrievePromise.success(document.getLong("length"))
          case _ => lengthRetrievePromise.failure(t)
        }
      })

    lengthRetrievePromise.future
  }
}
