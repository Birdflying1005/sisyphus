package my.thereisnospoon.sisyphus

import java.io.FileOutputStream

import com.mongodb.MongoClient
import com.mongodb.client.model.Sorts._
import org.bson.Document
import org.bson.types.Binary
import org.scalatest.{FlatSpec, Matchers}

import collection.JavaConverters._

class ReactiveGridFsTest extends FlatSpec with Matchers {

  val mongoUri = "mongodb://localhost:27017"

  "Mongo driver" should "be able to connect to GridFS" in {

    val mongoClient = new MongoClient
    val db = mongoClient.getDatabase("webm-haus")
    val chunks: java.lang.Iterable[Document] = db.getCollection("fs.chunks").find().sort(ascending("n"))

    var file: FileOutputStream = null
    try {
      file = new FileOutputStream("my.webm")
      chunks.asScala.map(_.get("data", classOf[Binary]).getData).foreach(file.write(_))
    } finally {
      file.close()
    }
    println("Done")
  }
}
