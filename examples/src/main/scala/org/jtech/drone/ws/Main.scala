package org.jtech
package drone
package ws

import java.util.UUID

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.Source
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka._
import org.reactivestreams.Publisher



import akka.http.scaladsl._
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.http.scaladsl.model.ws._


object Main extends App  with PingService {
  implicit val system = ActorSystem("DronWsSystem")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(system)

  scala.sys.ShutdownHookThread {
    system.log.info("shutting down the actor system")
    system.shutdown()
  }

  val routes = get {
    pathEndOrSingleSlash {
      getFromResource(s"web/ws-ascii-stream.html")
    } ~
      path("wsping") {
        getFromResource("web/ws-ping.html")
      } ~
      pathPrefix("ascii") {
        val consumerId = UUID.randomUUID()
        handleWebsocketMessages(AsciiService.findOrCreateFlow.websocketFlow(consumerId))
      } ~
      path("ping") {
        // handleWebsocketMessages method will upgrade
        // connections to websockets
        // using echoService handler
        handleWebsocketMessages(pingFlow)
      } ~
      getFromResourceDirectory("web")
  }


  val binding = Http().bindAndHandle(routes, settings.HttpInterface, settings.HttpPort)

  println(s"Server is now online at http://${settings.HttpInterface}:${settings.HttpPort}. \nPress RETURN to stop...")

  val service: AsciiService = AsciiService.findOrCreateFlow(system)


  val kafka = new ReactiveKafka()

  val publisher: Publisher[StringKafkaMessage] = kafka.consume(settings.kafka.kafkaConsumerSettings)

  Source(publisher).map(m ⇒ WsMessage(m.message())).map(m ⇒ service.sendMessage(m)).to(Sink.ignore).run()
}