package org.jtech
package drone
package feed

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.jsuereth.image.Ascii
import com.jsuereth.video._

object Main extends App{
  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val url = "file:///Users/remko/Dropbox/workspace/ordina/codeandcomedy/streamerz/examples/CharlietheUnicorn.mp4"

  val settings = Settings(actorSystem)

  // TODO: Create drone freed  source. At present it is being generated from the URL
  val webcamSource: Source[VideoFrame, Unit] = Source(com.jsuereth.video.WebCam.default(actorSystem))

  val one = webcamSource.take(1)

  val videoSource: Source[VideoFrame, Unit] =
    Source(
      com.jsuereth.video.ffmpeg.readVideoURI(
        new java.net.URI(url),
        actorSystem,
        playAudio = false
      )
    )

  AsciiStreamPublisher(settings.kafka.kafkaProducerSettings)
    .publishFlow(one, Ascii.toCharacterColoredAscii)
    .run()
}
