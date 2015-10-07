package org.jtech
package drone
package ws

import akka.http.scaladsl.server.Directives._


trait ApiRoutes {
  this: PingService ⇒

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
        handleWebsocketMessages(pingFlow)
      }
    }

}
