package org.jtech
package drone

import akka.actor._

import com.softwaremill.react.kafka._
import kafka.serializer._
import com.typesafe.config._
import net.ceedubs.ficus.Ficus._

class Settings(system: ExtendedActorSystem) extends Extension {
  private val config = system.settings.config.getConfig("ordina.jtech.drone")
  val HttpPort = config.as[Int]("ws.port")

  object kafka {
    val kafkaConsumerSettings = ConsumerProperties(
      brokerList =  config.as[String]("kafka.broker.hosts"),
      zooKeeperHost = config.as[String]("kafka.zookeeper.hosts"),
      topic = config.as[String]("kafka.topic.drone"),
      groupId =  config.as[String]("kafka.consumer.group.id"),
      decoder = new StringDecoder()
    )
    val kafkaProducerSettings =
      ProducerProperties(
        brokerList = config.as[String]("kafka.broker.hosts"),
        topic = config.as[String]("kafka.topic.drone"),
        encoder = new StringEncoder()
      )
  }
}

object Settings extends ExtensionKey[Settings] {
  def apply(context: ActorContext): Settings = apply(context.system)
}

trait SettingsProvider {
  val settings: Settings
}

trait DefaultSettingsProvider extends SettingsProvider with ActorRefFactoryProvider {
  lazy val settings: Settings = Settings(actorSystem)
}



