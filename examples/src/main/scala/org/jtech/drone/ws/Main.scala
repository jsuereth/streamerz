package org.jtech
package drone
package ws

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Directives
import akka.stream._
import akka.stream.scaladsl._

import scala.io._


object Main extends App {
  implicit val system = ActorSystem("DronWsSystem")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(system)

  scala.sys.ShutdownHookThread {
    system.log.info("shutting down the actor system")
    system.shutdown()
  }

  import Directives._

  val routes = get {
    pathEndOrSingleSlash {
      complete("Welcome to websocket server")
    }
  } ~
    path("ping") {
      get{
        // handleWebsocketMessages method will upgrade
        // connections to websockets
        // using echoService handler
        handleWebsocketMessages(pingService)
      }
    }

  val pingService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) ⇒ TextMessage("Pong: " + txt)
    case _                       ⇒ TextMessage("Message type unsupported")
  }

  val binding = Http().bindAndHandle(routes, settings.HttpInterface, settings.HttpPort)

  println(s"Server is now online at http://${settings.HttpInterface}:${settings.HttpPort}. \nPress RETURN to stop...")
  StdIn.readLine()

  import system.dispatcher
  binding.flatMap(_.unbind()).onComplete(_ ⇒ system.shutdown())
  println("service is down ...")
}