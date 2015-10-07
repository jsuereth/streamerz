package jtech.drone.feed

import java.awt.image.BufferedImage

import akka.actor._
import akka.stream.scaladsl._
import com.jsuereth.image.Ascii
import com.jsuereth.image.Ascii._
import com.jsuereth.video._
import com.softwaremill.react.kafka._
import kafka.serializer._
import org.reactivestreams._

object AsciiStreamPublisher {
  val kafka = new ReactiveKafka()

  val properties = ProducerProperties(
    brokerList = "localhost:9092", // currently it is publishing to localhost
    topic = "asciifreed",
    encoder = new StringEncoder()
  )

  def publishFlow(source: Source[VideoFrame, Unit], asciifier: BufferedImage ⇒ String = toCharacterColoredHtml)(implicit system: ActorSystem): RunnableGraph[Unit] = {
    val kafkaPublisher: Subscriber[String] = kafka.publish(properties)
    val sink: Sink[String, Unit] = Sink(kafkaPublisher)

    source.map(s ⇒ toCharacterColoredHtml(s.image)).to(sink)
  }

}
