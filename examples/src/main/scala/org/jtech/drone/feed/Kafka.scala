package org.jtech.drone.feed

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.softwaremill.react.kafka.{ProducerProperties, ReactiveKafka}
import org.reactivestreams.Subscriber

object Kafka {
  lazy val kafka = new ReactiveKafka()

  def kafkaSink(properties: ProducerProperties[String])(implicit system: ActorSystem): Sink[String, Unit] = {
    val kafkaPublisher: Subscriber[String] = kafka.publish(properties)
    Sink(kafkaPublisher)
  }

}
