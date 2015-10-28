

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.jsuereth",
  version := "0.1",
  scalaVersion := "2.11.7",
  connectInput in run := true
)



lazy val ansi =
  project.settings(commonSettings:_*)

lazy val ansimarkdown =
  project.settings(commonSettings:_*).dependsOn(ansi).settings(
    libraryDependencies ++= Seq(Deps.pegdown)
  )

lazy val ansiui =
   project.settings(commonSettings:_*).dependsOn(ansi).settings(libraryDependencies ++= Seq(Deps.reactiveCollections))
lazy val image =
  project.settings(commonSettings:_*).dependsOn(ansi)

lazy val video =
  project.settings(commonSettings:_*).dependsOn(image, ansi).settings(libraryDependencies += Deps.akkaStreams)

lazy val ffmpeg =
  project.settings(commonSettings:_*).settings(
   libraryDependencies ++= Seq(Deps.akkaStreams, Deps.xuggler),
   resolvers += "xuggler-repo" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java/"
 ).dependsOn(video)

lazy val webcam =
  project.settings(commonSettings:_*).settings(
    libraryDependencies ++= Seq(Deps.akkaStreams, Deps.webcam)
  ).dependsOn(video)

lazy val examples =
  project.settings(commonSettings:_*).dependsOn(image, ffmpeg, webcam).settings(
    fork in (Compile, run) := true,
    assemblyMergeStrategy in assembly := {
      case PathList("org", "xmlpull", xs @ _*)        => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    mainClass in assembly := Some("examples.AsciiVideo"),
    assemblyJarName in assembly := "console-roll.jar",
    libraryDependencies ++= Seq(Deps.akkaHttp, Deps.kafka, Deps.ficus, Deps.logging)
  )

lazy val slideui =
  project.settings(commonSettings:_*).dependsOn(ansiui, image, webcam, ansimarkdown, ffmpeg).settings(
    mainClass in assembly := Some("com.jsuereth.ansi.ui.TestUI"),
    libraryDependencies += Deps.scalaCompiler.value,
    assemblyMergeStrategy in assembly := {
      case PathList("org", "xmlpull", xs @ _*)        => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
