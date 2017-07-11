package my.tins.gcs.streaming

import com.google.cloud.storage.{BlobId, BlobInfo, StorageOptions}
import org.scalatest.FlatSpec

class MySpec extends FlatSpec {

  "API" should "allow creating blobs on GCS" in {

    val storageService = StorageOptions.getDefaultInstance.getService
    val blobId = BlobId.of("sisyphus-bkt", "object.txt")
    val blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build()
    val blob = storageService.create(blobInfo, "Hi GCS!".getBytes)
  }
}
