package org.jtech

import akka.actor._


package object drone {

  /**
   * Mixin trait for providing an implementation of the Akka ActorRefFactory trait.
   */
  trait ActorRefFactoryProvider {
    implicit def actorRefFactory: ActorRefFactory
    implicit def actorSystem: ActorSystem

    def actorSystem(implicit refFactory: ActorRefFactory): ExtendedActorSystem = {
      refFactory match {
        case x: ActorContext        ⇒ actorSystem(x.system)
        case x: ExtendedActorSystem ⇒ x
        case x                      ⇒ throw new IllegalArgumentException("Unsupported ActorRefFactory implementation: " + refFactory)
      }
    }
  }

  /**
   * Implementation of ActorRefFactoryProvider for mixing into Actors.
   */
  trait ActorRefFactoryProviderForActors extends ActorRefFactoryProvider { this: Actor ⇒
    def actorRefFactory = context
  }
}
