package org.jtech
package drone
package feed

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.jsuereth.image.Ascii
import com.jsuereth.video._

object Main extends App{
  val url = "file:///Users/adilakhter/projects/dev/streamerz/examples/BlackBerry.mp4"

  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(actorSystem)

  // TODO: Create drone freed  source. At present it is being generated from the URL
  val videoSource: Source[VideoFrame, Unit] =
    Source(
      com.jsuereth.video.ffmpeg.readVideoURI(new java.net.URI(url),
      actorSystem,
      playAudio = false))


  AsciiStreamPublisher(settings.kafka.kafkaProducerSettings)
    .publishFlow(videoSource, Ascii.toCharacterColoredAscii)
    .run()
}
