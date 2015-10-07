package org.jtech
package drone
package ws

import akka.actor._
import akka.http.scaladsl._
import akka.stream._
import scala.io._


object Main extends App with HttpService {
  implicit val system = ActorSystem("DronWsSystem")
  implicit val materializer = ActorMaterializer()

  val settings = Settings(system)

  scala.sys.ShutdownHookThread {
    system.log.info("shutting down the actor system")
    system.shutdown()
  }

  val binding = Http().bindAndHandle(routes, settings.HttpInterface, settings.HttpPort)

  println(s"Server is now online at http://${settings.HttpInterface}:${settings.HttpPort}. \nPress RETURN to stop...")
  StdIn.readLine()

  import system.dispatcher
  binding.flatMap(_.unbind()).onComplete(_ ⇒ system.shutdown())
  println("service is down ...")
}