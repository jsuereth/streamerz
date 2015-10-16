package org.jtech.drone.ws

import java.util._

import akka.actor._
import akka.http.scaladsl._
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server._

import akka.stream._
import akka.stream.scaladsl._

trait AsciiService {
  def websocketFlow(consumerId: UUID): Flow[Message, Message, _]

  def sendMessage(event: WsEvent): WsEvent
}

object AsciiService {
  var asciiServiceOpt: Option[AsciiService] = None

  def findOrCreateFlow(implicit actorSystem: ActorSystem): AsciiService =
    asciiServiceOpt.getOrElse(createAsciiService)

  private def createAsciiService(implicit actorSystem: ActorSystem): AsciiService = {
    println(s"instantiating new AsciiService")
    val asciiService = BasicAsciiService(actorSystem)
    asciiServiceOpt = Some(asciiService)

    asciiService
  }
}

class BasicAsciiService(actorSystem: ActorSystem) extends AsciiService {
  private[this] val serviceActor = actorSystem.actorOf(Props(classOf[AsciiServiceActor]))

  override def websocketFlow(consumerId: UUID): Flow[Message, Message, _] =
  Flow(Source.actorRef[WsMessage](bufferSize = 5, OverflowStrategy.dropTail)) { implicit builder ⇒ wsSource ⇒

    val fromWebSocket = builder.add(
      Flow[Message].collect {
        case TextMessage.Strict(txt) ⇒ WsMessage(txt)
      }
    )

    val backToWebsocket = builder.add(
      Flow[WsMessage].map {
        case WsMessage(ascii) ⇒ TextMessage(ascii)
      }
    )
    
    val asciiServiceActorSink = Sink.actorRef[WsEvent](serviceActor, UserLeft(consumerId))

    val merge = builder.add(Merge[WsEvent](2))

    val asciiActorSource = builder.materializedValue.map(actor ⇒ UserJoined(consumerId, actor))

    fromWebSocket ~> merge.in(0)

    asciiActorSource ~> merge.in(1)

    merge ~> asciiServiceActorSink

    wsSource ~> backToWebsocket

    (fromWebSocket.inlet, backToWebsocket.outlet)
  }

  def sendMessage(e: WsEvent): WsEvent = {
    serviceActor ! e
    e
  }
}

object BasicAsciiService {
  def apply(implicit actorSystem: ActorSystem) =
    new BasicAsciiService(actorSystem)
}
