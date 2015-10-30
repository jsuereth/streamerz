package org.jtech
package drone
package feed

import scala.language.postfixOps
import scala.concurrent.duration._

import java.awt.image.BufferedImage

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import org.jtech.drone.Ascii._

import com.jsuereth.image.Resizer
import com.jsuereth.video._

object AsciiImageProducer extends App {
  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(actorSystem)

  def resize(img: BufferedImage): BufferedImage = Resizer.forcedScale(img, 80, 60)

  var lastTime = System.currentTimeMillis

  def limitFramerate[T](t: T): Boolean = {
    val currentTime = System.currentTimeMillis
    if (currentTime - lastTime > 32) {
      lastTime = currentTime
      true
    } else false
  }

  val mode =
    if (args.length == 1 && args(0) == "drone") "drone"
    else "webcam"

    val source: Source[BufferedImage, Unit] = mode match {
      case "webcam" => Source(com.jsuereth.video.WebCam.default(actorSystem))
        .filter(limitFramerate)
        .map(_.image)
      case "drone"  => Source(DroneCamera.default(actorSystem))
        .filter(limitFramerate)
    }

    source
      .map(resize)
      .map(correctFormat)
      .map(asciify)
      .map(toJSON)
      .map(compress)
      .map(toBase64)
      .to(Kafka.kafkaSink(settings.kafka.kafkaProducerSettings))
      .run()
}
