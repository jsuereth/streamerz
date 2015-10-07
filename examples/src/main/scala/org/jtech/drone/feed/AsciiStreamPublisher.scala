package org.jtech
package drone
package feed

import java.awt.image._

import akka.actor._
import akka.stream.scaladsl._
import com.jsuereth.image.Ascii._
import com.jsuereth.video._
import com.softwaremill.react.kafka._
import org.reactivestreams._

case class  AsciiStreamPublisher(properties: ProducerProperties[String]) {
  val kafka = new ReactiveKafka()

  def publishFlow(source: Source[VideoFrame, Unit], asciifier: BufferedImage ⇒ String = toCharacterColoredHtml)(implicit system: ActorSystem): RunnableGraph[Unit] = {

    val kafkaPublisher: Subscriber[String] = kafka.publish(properties)
    val sink: Sink[String, Unit] = Sink(kafkaPublisher)

    source.map(s ⇒ toCharacterColoredHtml(s.image)).to(sink)
  }
}
