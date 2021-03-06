name := "streaming"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.akka" %% "akka-http" % "10.0.4",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.9",

  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.4" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "com.github.tomakehurst" % "wiremock" % "2.5.1" % Test
)
