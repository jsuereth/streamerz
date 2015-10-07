package com.jsuereth.video

import java.io.PrintStream
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.stream.actor._
import com.jsuereth.ansi._

import org.reactivestreams._




object Terminal {
  /** Renders a video to stdout.
    *
    * Note: This method is dumb and just assumes it can own the terminal and take over all system.out.
    */
  def terminalMoviePlayer(system: ActorSystem): Subscriber[AsciiVideoFrame] = ansiMoviePlayer(System.out, system)

  /** Renders a video to the output stream.
    *
    * Note: This method is dumb and just assumes it can own the terminal and take over all system.out.
    */
  def ansiMoviePlayer(out: PrintStream, system: ActorSystem): Subscriber[AsciiVideoFrame] =
    ActorSubscriber(system.actorOf(TerminalRenderActor.props(out)))
}



object TerminalRenderActor{
  def props(out: PrintStream): Props = Props(new TerminalRenderActor(out))
}
class TerminalRenderActor(out: PrintStream) extends ActorSubscriber {
  override val requestStrategy = OneByOneRequestStrategy
  private var last = System.nanoTime()
  private var lastTick = 0L
  // TODO - when initializing we should clear the screen.
  out.print(Ansi.CLEAR_SCREEN)
  def receive: Receive = {
    case ActorSubscriberMessage.OnNext(frame: AsciiVideoFrame) =>
      // Throttle and render
      throttleToRealtime(frame.timeStamp, frame.timeUnit)
      //Now render
      out.print(Ansi.MOVE_CURSOR_TO_UPPER_LEFT)
      out.print(frame.image)
      out.flush()
    case ActorSubscriberMessage.OnComplete =>
      out.print(Ansi.RESET_COLOR)
      out.print(Ansi.CLEAR_SCREEN)
      out.flush()
    case ActorSubscriberMessage.OnError(e) =>
      out.print(Ansi.RESET_COLOR)
      out.print(Ansi.CLEAR_SCREEN)
      out.flush()
  }

  // TODO - put this somewhere more useful.
  private def throttleToRealtime(frameTime: Long, tickUnit: TimeUnit) = {
    // Here is some gunk to slow down rendering to the appropriate frame rate.
    concurrent.blocking {
      val tick = TimeUnit.NANOSECONDS.convert(frameTime, tickUnit)
      val time = System.nanoTime()
      // Ensure we aren't just starting off.
      if((lastTick > 0L) && (lastTick < tick)) {
        val diff = tick - lastTick
        val endWait = last + diff
        // TODO - we busy spin for now, forcing the thread somewhere else
        // Maybe not the best way to do this.
        while(System.nanoTime < endWait) {
          val remaining = endWait - System.nanoTime
          // TODO - we should measure the cost of each of these strategies instead of making it up on the fly.
          val remainingMS = remaining / 1000000
          if(remainingMS > 2) Thread.sleep(remainingMS, (remaining % 1000000).toInt)
          else if (remaining > 1000) Thread.`yield`()
          else ()  // Busy loop
        }
      }
      lastTick = tick
      last = System.nanoTime
    }
  }
}
