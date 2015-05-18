package com.jsuereth.ansi.ui

import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

import com.jsuereth.ansi.ui.internal.{NonblockingInputStreamWrapper, NonBlockingInputStreamWrapper}

import scala.util.matching.Regex


/**
 * Helper class which will read input off of stdin (using java's awesome blocking API) and fire them into the
 * main UI event loop.  This, ideally, leads to a non-blocking-ish API.
 *
 * @param e  THe event loop
 * @param in  The input stream we should use as stdin.
 */
// TODO - This should probably be more sophisticated, e.g. attempting to distinguish CTRL characters and the like.
//        Actually, there should be some kind of "keypress->event" mapping we could handle.
private[ui] class InputReadingThread(e: MainUILoop, in: java.io.InputStream = System.in) extends Thread(s"input-reading-thread") {
  val nonBlockingInput = new NonBlockingInputStreamWrapper(in)
  private val running = new AtomicBoolean(true)
  override def run(): Unit = {
    while(running.get) {
      // TODO - Catch key escapes
      e.fire(readNextInput())
    }
  }
  def close(): Unit = {
    running.set(false)
  }


  /** Special handler which can handle special input and place it into an event appropriately. */
  private def readNextInput(): Event = {
    nonBlockingInput.read() match {
      case 27 =>
        // We need to special handle escape...
        // TODO - If we don't find an escape sequence, that means that just ESC was pressed...
        readEscapeSequence()
      case n => Key(n)
    }
  }

  private def readEscapeSequence(): Event = {
    val CursorPositionExcape = new Regex("([0-9]+);([0-9]+)R")
    // Here we set a timeout on the read so we can try to detect an "ESC" press vs some other key.
    nonBlockingInput.read(timeout = 40L) match {
        // Read timeout value, this means an escape was pressed, as there is no follow-on characters.
      case NonblockingInputStreamWrapper.READ_TIMEOUT => EscapeKey()
      case '[' =>
        // TODO -
        readAnsiCode() match {
          case CursorPositionExcape(row, col) => CursorPosition(row.toInt,col.toInt)
          case s => EscapeCode(s)
        }
      case n if (n >= 64) && (n <= 95) => EscapeCode(s"$n")
      case n =>
        // TODO - It turns out CTRL/ALT + key => leads to this scenario.  We may want better understanding of this scenario, as we may
        //        be corrupting our input streams by not reading the correct number of characters.
        //        It appears that we may be capturing "alt" or "ctl" escape, but not capturing the key that was pressed with the
        //        CTRL/ALT flag.
        EscapeCode(readNonAnsiCode())
    }
  }

  private def readNonAnsiCode(sb: StringBuilder = new StringBuilder): String = {
    nonBlockingInput.read.toChar match {
      case n if (n >= '@') || (n <= '~') => sb.append(n).toString
      case n => sb.append(n); readNonAnsiCode(sb)
    }
  }


  private def readAnsiCode(sb: StringBuilder = new StringBuilder): String = {
    nonBlockingInput.read.toChar match {
      case term if (term > '@') && (term < '~') => sb.append(term).toString()
      case n => sb.append(n); readAnsiCode(sb)
    }
  }
}