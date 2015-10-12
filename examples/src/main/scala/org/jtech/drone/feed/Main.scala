package org.jtech
package drone
package feed

import java.awt.image.BufferedImage

import akka.actor._
import com.jsuereth.image.{Ascii, Resizer}
import com.jsuereth.video._

import scala.language.postfixOps

import akka.stream.scaladsl._
import akka.stream._
import scala.concurrent.duration._

object Main extends App{
  implicit val actorSystem = ActorSystem("StreamPublisher")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(actorSystem)

  def resize(img: BufferedImage): BufferedImage = Resizer.preserveRatioScale(img, 80, 60)

  def throttle[T](rate: FiniteDuration): Flow[T, T, Unit] = {
    Flow() { implicit builder ⇒
      import akka.stream.scaladsl.FlowGraph.Implicits._
      val zip = builder.add(Zip[T, Unit.type]())
      Source(rate, rate, Unit) ~> zip.in1
      (zip.in0, zip.out)
    }.map(_._1)
  }




  // TODO: Create drone feed source. At present the source is the webcam
  val webcamSource: Source[VideoFrame, Unit] = Source(com.jsuereth.video.WebCam.default(actorSystem))

  webcamSource
    .via(throttle(200 millis))
    .map(_.image)
    .map(resize)
    .map(Ascii.toCharacterColoredHtml)
    .to(Kafka.kafkaSink(settings.kafka.kafkaProducerSettings))
    .run()
}
