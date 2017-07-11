name := "gcs-streaming"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-storage" % "1.2.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.3",

  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)