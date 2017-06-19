name := "protocol"

scalaVersion := "2.12.1"

version := "0.1.0"

organization := "my.tins"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
