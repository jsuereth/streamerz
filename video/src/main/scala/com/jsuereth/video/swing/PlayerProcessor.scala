package com.jsuereth.video
package swing



import akka.actor.{ActorSystem, Props, ActorRef}
import akka.stream.actor._
import org.reactivestreams.{Subscriber, Publisher}

case class PlayerRequestMore(elements: Long)
case object PlayerDone


/** Construct a processor of UI events. */
object PlayerProcessor {
  /** Creates a new processor which can handle incoming UIControls and output VideoFrames. */
  def create(system: ActorSystem, openVideo: () => Publisher[VideoFrame]): (Subscriber[UIControl], Publisher[VideoFrame]) = {
    val playEngineActor = system.actorOf(Props(new PlayerProcessorActor(openVideo)))
    val playEngineConsumer = ActorSubscriber[UIControl](playEngineActor)
    val playEngineProducer = ActorPublisher[VideoFrame](playEngineActor)
    (playEngineConsumer, playEngineProducer)
  }
}
/** An actor which is responsible for taking Play/Pause/Stop requests and adjusting
  *  the output frame stream accordingly.
  *
  *  This player
  */
class PlayerProcessorActor(openVideo: () => Publisher[VideoFrame]) extends ActorPublisher[VideoFrame] with ActorSubscriber /*[UIControl]*/ {
  // Ensure that we only ask for UI events one at a time.
  override val requestStrategy = OneByOneRequestStrategy
  // Private data, holding which actor is currently acting as a Consumer[Frame] for us.
  private var currentPlayer: Option[ActorRef] = None
  // Whether or not the stream is in a pause state.
  // TODO - not provided initially for exercise?
  private var isPaused: Boolean = false

  /** The main message loop of the actor.  All our behavior/state changes reside in here. */
  override def receive: Receive = {
    case ActorSubscriberMessage.OnNext(control: UIControl) =>
      // EXERCISE - Implement handling of UI control messages.
      //     On any control message, we need to update our state appropriately.
      //     Play - we need to ensure that we have a bridge to a Producer[Frame] which
      //            which will play back our file.   (See `kickOffFileProducer`).
      //            Additionally, we need to make sure we *ask* for data from the player
      //            once we ourselves have been asked for data.
      //     Pause - we need to modify our state so that we do not ask for any more
      //             Frame messsages from the underlying `currentPlayer`.
      //     Stop  -  We need to cancel the currentPlayer and update our state.
      control match {
        case Play =>
          // Update state and kick off the stream
          if(currentPlayer.isEmpty) kickOffFileProducer()
          isPaused = false
          // TODO - If we have pause cache, we should fire those events.
          requestMorePlayer()
        case Pause =>
          isPaused = true
        case Stop =>
          isPaused = false
          currentPlayer.foreach(_ ! Stop)
          currentPlayer = None
      }
    case ActorPublisherMessage.Request(elements) =>
      // EXERCISE - Implement handling of request for more elements.
      //    When the downstream asks for more elements, we should delegate these
      //    down to our underlying `currentPlayer`.  However, if we are in pause mode,
      //    then we should not ask for more Frames.
      //  Hint:  The key to backpressure in reactive streams is that we cannot send any frames
      //           until after this message is received.
      //  Hint2:  If you're running out of memory, remember to flush any buffers you might have.
      if(!isPaused) {
        if(tryEmptyBuffer()) requestMorePlayer()
      }
    case PlayerDone =>
      // EXERCISE - Implement what to do when the file is complete.
      //    When the file completes you have the following choices:
      //    1. send the completion message downstream.
      //    2. kill the curentPlayer and create a new one, looping back to the begging.
      //    3. Issue an error, just to see what happens.
      kickOffFileProducer()
      requestMorePlayer()
    case f: VideoFrame =>
      // EXERCISE - Implement what we do when the underlying stream pushes us some data.
      //
      //            Hint:  Look at the totalDemand member of ActorProducer trait.
      //            Hint2:  If you see runtime errors, make sure you're only pushing data after
      //                    the consumer has asked for it.  Also, check out the private `buffer` methods.
      if(totalDemand > 0) onNext(f)
      else buffer(f)
  }





  // Buffering (unbounded) if we get overloaded on input frames.
  private val buf = collection.mutable.ArrayBuffer.empty[VideoFrame]
  /** Buffers the given frame to be pushed to future clients later. */
  private def buffer(f: VideoFrame): Unit = buf.append(f)
  /** Attempts to empty the buffer of frames we couldn't yet send, if there is
    *  demand from consumers.   Returns true if the entire buffer was emptied, false otherwise.
    */
  private def tryEmptyBuffer(): Boolean = {
    while(!buf.isEmpty && totalDemand > 0)
      onNext(buf.remove(0))
    buf.isEmpty
  }
  /** Creates a new `Producer[Frame]` for the underlying file, and feeds its `Frame` output
    *  to ourselves as raw `Frame` messages
    */
  private def kickOffFileProducer(): Unit = {
    val producer = openVideo()
    val consumerRef = context.actorOf(Props(new PlayerActor(self)))
    currentPlayer = Some(consumerRef)
    producer subscribe ActorSubscriber(consumerRef)
  }
  /** Request as much data from the underlying Producer[Frame] as our client consumers
    *  have requests from us.
    */
  private def requestMorePlayer(): Unit = {
    if(totalDemand > 0) currentPlayer match {
      case Some(player) => player ! PlayerRequestMore(totalDemand)
      case None => ()
    }
  }
}
/** A helper actor which forwards requests from the file-reading consumer back to
  *  the main controlling actor.
  *
  *  This just forwards `Frame` and `PlayerDone` messages to the main actor.  In addition,
  *  any `PlayerRequestMore` messages are delegated down as actual akka stream `requestMore` calls.
  */
class PlayerActor(consumer: ActorRef) extends ActorSubscriber {
  // All requests for more data handled by our 'consumer' actor.
  override val requestStrategy = ZeroRequestStrategy
  override def receive: Receive = {
    // Just delegate back/forth with the controlling 'consumer' actor.
    case ActorSubscriberMessage.OnNext(frame: VideoFrame) =>
      consumer ! frame
    case PlayerRequestMore(e) => request(e)
    case Stop => cancel()
    case ActorSubscriberMessage.OnComplete =>
      consumer ! PlayerDone
  }
}