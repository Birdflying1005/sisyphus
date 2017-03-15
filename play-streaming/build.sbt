name := "play-streaming"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test
)
