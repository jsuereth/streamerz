# Reactive (and Akka) Streams

```scala
val system = ActorSystem()
val rickRollVideo = Source(ffmpeg.readVideoURI(new java.net.URI(url), system, playAudio = true))
val terminal = Sink(terminalMoviePlayer(system))
asciifier.to(terminal).runWith(rickRollVideo)(FlowMaterializer(settings))
```
