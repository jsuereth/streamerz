package com.jsuereth.ansi.ui

import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

import com.jsuereth.ansi.ui.internal.NonBlockingInputStreamWrapper

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
      case n => KeyPress(n)
    }
  }

  private def readEscapeSequence(): Event = {
    val CursorPositionExcape = new Regex("([0-9]+);([0-9]+)R")
    // Here we set a timeout on the read so we can try to detect an "ESC" press vs some other key.
    nonBlockingInput.read(timeout = 40L) match {
        // Read timeout value, this means an escape was pressed.
      case -2 => KeyPress(27)
      case '[' =>
        // TODO - We should consume until an ANSI terminator, and then check the code.
        readAnsiCode() match {
          case "A" => UpKey()
          case "B" => DownKey()
          case "C" => RightKey()
          case "D" => LeftKey()
          case "F" => End()
          case "H" => Home()
          case CursorPositionExcape(row, col) => CursorPosition(row.toInt,col.toInt)
          case s => UnknownAnsiCode(s)
        }
      // TODO - This means escape was pressed.  Here we don't want to consume the n, we just want to
        // fire the ESC and then read the n.
      case n if (n >= 64) && (n <= 95) =>
        UnknownEscape(n)
        // TODO - A timeout for this, so we can know just ESC was pressed.
      case n =>
        // Here, we may be able to assume 'alt' was held before typign the character, as it's a known thing linux terminals do.
        e.fire(DisplayText(s"Expected esacpe code, found char $n"))
        // TODO - ??? we need to expand our handling of keys.
        sys.error(s"Expecting escape code, found $n")
    }
  }


  private def readAnsiCode(sb: StringBuilder = new StringBuilder): String = {
    nonBlockingInput.read.toChar match {
      case term if (term > '@') && (term < '~') => sb.append(term).toString()
      case n => sb.append(n); readAnsiCode(sb)
    }
  }
}