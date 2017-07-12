name := "gcs-streaming"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-storage" % "1.2.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",

  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.3" % Test
)