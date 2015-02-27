

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.jsuereth",
  version := "0.1",
  scalaVersion := "2.11.5"
)


lazy val ansi =
  project.settings(commonSettings:_*)

lazy val ansiui =
   project.settings(commonSettings:_*).dependsOn(ansi).settings(libraryDependencies += Deps.jansi)

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
    assemblyJarName in assembly := "console-roll.jar"
  )
