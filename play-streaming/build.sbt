name := "play-streaming"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1"
