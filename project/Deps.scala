import sbt.Keys._
import sbt._


object Deps {
  val akkaStreams = "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M2"
  val xuggler = "org.boofcv" % "xuggler" % "0.17"
  val webcam = "com.github.sarxos" % "webcam-capture" % "0.3.9"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.0"
  val jansi = "org.fusesource.jansi" % "jansi" % "1.11"
  val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
  val scalaCompiler = Def.setting { "org.scala-lang" % "scala-compiler" % scalaVersion.value }
  val reactiveCollections = "com.storm-enroute" %% "reactive-collections" % "0.5"
  val pegdown = "org.pegdown" % "pegdown" % "1.5.0"
}