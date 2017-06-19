name := "upload"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.4",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "commons-codec" % "commons-codec" % "1.10",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.9",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "my.tins" %% "protocol" % "0.1.0",

  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.4" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "com.github.tomakehurst" % "wiremock" % "2.5.1" % Test
)
