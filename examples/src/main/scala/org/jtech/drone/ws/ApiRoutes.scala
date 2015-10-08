package org.jtech
package drone
package ws

import akka.http.scaladsl.server.Directives._


trait ApiRoutes {
  this: PingService ⇒

  val routes = get {
      pathEndOrSingleSlash {
        getFromResource("web/ws-ping.html") // TODO: change it to ascii stream 
      } ~
      getFromResourceDirectory("web")
    } ~
    path("wsping"){
      get{
        getFromResource("web/ws-ping.html")
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
