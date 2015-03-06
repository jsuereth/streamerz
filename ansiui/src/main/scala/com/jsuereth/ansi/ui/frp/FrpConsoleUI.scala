package com.jsuereth.ansi
package ui
package frp

import java.util.TimerTask
import com.jsuereth.ansi.ui.frp.layout.{Padding, ConsoleLayout}
import org.fusesource.jansi.AnsiString

import scala.reactive.Reactive.Subscription
import scala.reactive.{Signal, Reactive}

/**
 * A class which sets up the raw FRP-based infrastructure for a ConsoleUI.
 *
 * This UI is based on reactive-collections, which allow you to set up events/signals/etc. to drive UI-changes in
 * an efficient manner.
 */
final class FrpConsoleUI {

  /** An emitter for all events which get fired out of MainUILoop. */
  val events = new Reactive.Emitter[Event]
  private object dispatcher extends EventDispatcher {
    /** handle an event, globally, using this event dispatcher. */
    override def dispatch(e: Event): Unit = {
      events += e
    }
  }

  private val mainEventLoop = MainUILoop(dispatcher)
  /** An emitter for all 'render' events. Feeds a set of ANSI text blocks to the terminal. */
  val renders = new Reactive.Emitter[DisplayText]

  // Bridge the events from render into the main loop.
  private val subRender = renders foreach { text =>
    mainEventLoop.fire(text)
  }
  val runnables = new Reactive.Emitter[GenericRunnable]
  private val subRunable = runnables foreach { e =>
    mainEventLoop.fire(e)
  }


  val consoleSize: Signal[ConsoleResize] = {
    val e = events collect {
      case x: ConsoleResize => x
    }
    e.signal(ConsoleResize(0,0))
  }

  /** Takes ownership of the current thread and starts running the UI. */
  def run(): Unit = mainEventLoop.run()


  /** Constructs a text label using a signal for the string, and the given layout object. */
  def label(text: Signal[String], layout: Signal[ConsoleLayout]): Signal[Unit] with Subscription = {
    // TODO - Allow labels to be left/right/centered
    (text zip layout) { (t, l) =>
      val lines = t.split("[\r\n]+").take(l.size.height)
      val padLines = Padding.padLines(l.size.height-lines.length, l.size.width)
      val renderable =
        for((line, idx) <- (padLines ++ lines).zipWithIndex) yield {
          val realSize = new AnsiString(line).length
          val col = l.pos.col
          val row = l.pos.row + idx
          val pad = Padding.pad(l.size.width - realSize)
          // TODO - Truncate length
          val padded = s"$pad$line"
          s"${Ansi.MOVE_CURSOR(row, col)}${Ansi.RESET_COLOR}$padded"
        }
      renders += DisplayText(s"${Ansi.SAVE_CURSOR_POSITION}${renderable.mkString("")}${Ansi.RESTORE_CURSOR_POSITION}")
    }

  }

}
