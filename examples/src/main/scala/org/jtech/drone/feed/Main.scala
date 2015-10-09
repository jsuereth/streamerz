package org.jtech
package drone
package feed

import java.awt.image.BufferedImage

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.jsuereth.image.{Ascii, Resizer}
import com.jsuereth.video._

object Main extends App{
  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(actorSystem)

  def resize(img: BufferedImage): BufferedImage = Resizer.preserveRatioScale(img, 80, 60)

  // TODO: Create drone feed source. At present the source is the webcam
  val webcamSource: Source[VideoFrame, Unit] = Source(com.jsuereth.video.WebCam.default(actorSystem))

  webcamSource
//    .take(1) // for debugging
    .map(_.image)
    .map(resize)
    .map(Ascii.toCharacterColoredHtml)
    .to(Kafka.kafkaSink(settings.kafka.kafkaProducerSettings))
    .run()
}
