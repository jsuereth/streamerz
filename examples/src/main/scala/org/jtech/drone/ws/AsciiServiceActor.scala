package org.jtech.drone.ws

import java.util.UUID

import akka.actor.{ ActorRef, Actor }

class AsciiServiceActor extends Actor {
  var participants: Map[UUID, ActorRef] = Map.empty[UUID, ActorRef]

  override def receive: Receive = {
    case UserJoined(id, actorRef) ⇒
      participants += id → actorRef
      println(s"user $id joined")

    case UserLeft(id) ⇒
      participants -= id
      println(s"User $id left")

    case w @ WsMessage(_) ⇒
      broadcast(w)

    case _@ err ⇒
      println(s"Unhandled message $err")
  }

  def broadcast(w: WsMessage): Unit = {
    participants.values.foreach(_ ! w)
  }
}

sealed trait WsEvent

case class GenericMessage(image: String) extends WsEvent

case class UserJoined(id: UUID, userActor: ActorRef) extends WsEvent

case class UserLeft(id: UUID) extends WsEvent

case class WsMessage(img: String) extends WsEvent

