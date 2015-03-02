package com.jsuereth.ansi
package ui

import java.io.InputStream
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, ConcurrentLinkedQueue}
import java.util.concurrent.atomic.AtomicBoolean





/**
 * Main event loop for an ANSI UI.  Note, this tries to take ownership of System.in/System.out.  You should not
 * output directly to these anymore, but instead feed output into this event loop.
 *
 * The event loop will also spawn a thread which performs blocking reads of terminal input, as well as performing
 * any action which requires rendering to the screen.
 */
trait MainUILoop {
  /** Places an item into the event loop. This method is thread safe. */
  def fire(e: UILoopItem): Unit

  // TODO - register a listener for events on this loop.

  /** Runs the event loop.  Note:  This will block the thread it is run against until the exit event is encountered. */
  def run(): Unit
}

object MainUILoop {
  def apply(dispatcher: EventDispatcher): MainUILoop = new DefaultUILoop(dispatcher)
}

/** A default implementation of the event loop, designed for Unix-like systems.
  *
  * This loop will do two things when run:
  */
class DefaultUILoop(dispatcher: EventDispatcher, console: java.io.Console = System.console) extends MainUILoop {

  private val events: BlockingQueue[UILoopItem] = new LinkedBlockingQueue[UILoopItem]()
  // TODO - do we need to, in any way, disable System.out calls?
  private val stdout = System.out

  // TODO - disable System.in, or just assume well-behaved programs?

  override def fire(e: UILoopItem): Unit =
    events.put(e)


  override def run(): Unit = {
    Stty.disableEcho()
    Stty.bufferByCharacter()

    // REgister a restoration hook.
    object EnableEcho extends Thread {
      override def run(): Unit =
        Stty.enableEcho()
    }
    Runtime.getRuntime.addShutdownHook(EnableEcho)

    // Init the console, and spin up the terminal character reading thread.
    val inputReader = new InputReadingThread(this)
    inputReader.start()


    // TODO - for the event loop, we may want to additionally check to see if the terminal has resized and fire that event.
    def loop(): Unit =
      events.take() match {
        case e => process(e); loop()
      }
    loop()
  }
  def process(e: UILoopItem): Unit = {
    e match {
      case GenericRunnable(action) =>
        // TODO - Catch exceptions, or allow arbitrary runnnables to destroy the event loop?
        action.run()
      case DisplayText(text) =>
        //  Display text right now.  Should we flush?
        stdout.print(text)
      case e: Event =>
        dispatcher.dispatch(e)

    }
  }
}