package my.thereisnospoon.sisyphus.streaming

import com.mongodb.MongoClient
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Sorts._
import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId

object GridFsClient {

  val mongoUri = "mongodb://localhost:27017"
  val dbName = "webm-haus"

  private val mongoClient = new MongoClient
  private val db = mongoClient.getDatabase(dbName)
  private val chunksCollections = db.getCollection("fs.chunks")

  def retrieveChunksForFile(fileId: String): FindIterable[Document] =
    chunksCollections.find(Filters.eq("files_id", new ObjectId(fileId))).sort(ascending("n"))
}
