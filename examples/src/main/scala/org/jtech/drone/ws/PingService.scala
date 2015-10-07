package org.jtech.drone.ws

import akka.stream.scaladsl._
import akka.http.scaladsl.model.ws._


trait PingService {
  val pingFlow: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) ⇒ TextMessage("Pong: " + txt)
    case _                       ⇒ TextMessage("Message type unsupported")
  }
}
