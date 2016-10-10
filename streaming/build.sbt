lazy val root = (project in file(".")).
  settings(
    name := "streaming",
    version := "0.1.0-SNAPSHOT",
    organization := "my.thereisnospoon",
    scalaVersion := "2.12.0-M5",
    libraryDependencies += "org.scala-lang" % "scala-library" % "2.12.0-M5",
    libraryDependencies += "org.mongodb" % "mongodb-driver-async" % "3.2.2",
    libraryDependencies += "org.mongodb" % "mongodb-driver" % "3.2.2",
    libraryDependencies += "com.typesafe.akka" % "akka-actor_2.12.0-M5" % "2.4.8",
    libraryDependencies += "com.typesafe.akka" % "akka-stream_2.12.0-M5" % "2.4.8",
    libraryDependencies += "com.typesafe.akka" % "akka-http-experimental_2.12.0-M5" % "2.4.8",
    libraryDependencies += "org.scalatest" % "scalatest_2.12.0-M5" % "3.0.0" % Test
  )

resolvers in Global += "Sbt plugins" at "https://dl.bintray.com/sbt/sbt-plugin-releases"

