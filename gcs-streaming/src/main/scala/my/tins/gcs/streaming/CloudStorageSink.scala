package my.tins.gcs.streaming

import akka.stream.SinkShape
import akka.stream.stage.GraphStage
import akka.util.ByteString

class CloudStorageSink extends GraphStage[SinkShape[ByteString]]{

}
