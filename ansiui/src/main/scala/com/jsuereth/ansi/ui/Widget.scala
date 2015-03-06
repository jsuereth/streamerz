package com.jsuereth.ansi.ui

import java.awt.Color

import com.jsuereth.ansi.Ansi
import com.jsuereth.ansi.ui.frp.layout.{ConsoleSize, ConsolePosition}
import org.fusesource.jansi.AnsiString

import scala.reflect.ClassTag








/**
 * A widget that renders via ANSI codes to a location on the string.
 */
abstract class Widget(val pos: ConsolePosition) {
  /** An ANSI widget should render its content here. Note: This method is responsible for moving the cursor
    * and writing the text at the appropriate locations.
    */
  protected def content: String
  /** A string that can be written to the console which will save the cursor position and restore it. */
  def renderString: String =
    s"${Ansi.SAVE_CURSOR_POSITION}$content${Ansi.RESTORE_CURSOR_POSITION}"
}

// A dummy test widget that dumps logs intoa  fixed position on the screen in a thread.
class TestLogWidget extends Thread("test-log-widget") {
  val logs = new LogWidget(ConsolePosition(5,80), ConsoleSize(50,10))

  private def appendRandomLog(): Unit = {
    (Math.random() * 3).toInt match {
      case 0 => logs.appendLine(s"${Ansi.BOLD}${Ansi.BLUE}INFO${Ansi.RESET_COLOR} New stuff coming in")
      case 1 =>logs.appendLine(s"${Ansi.BOLD}${Ansi.YELLOW}WARN${Ansi.RESET_COLOR} Bad times coming in")
      case 2 => logs.appendLine(s"${Ansi.BOLD}${Ansi.CYAN}DEBUG${Ansi.RESET_COLOR} We're doing something great here")
      case _ => logs.appendLine(s"${Ansi.BOLD}${Ansi.RED}ERROR${Ansi.RESET_COLOR} Unknown!")
    }
  }
  private def appendANdRender(): Unit = {
    appendRandomLog()
    System.out.print(logs.renderString)
  }

  override def run(): Unit =  try {
    System.out.println(s"Runnning logs...")
    for(_ <- 0 until 1000) {
      appendANdRender()
      Thread.sleep(1000L)
    }
  } catch {
    case e: Exception =>
      e.printStackTrace()
  }
}


/** This is an ANSI widget which contains a ring buffer of log messages to render.
  *
  *
  * @param pos  THe position on the terminal for this widget
  * @param size  THe size of the widget
  *
  * Note: currently the widget system does not handle console resizes.
  */
class LogWidget(pos: ConsolePosition, size: ConsoleSize) extends Widget(pos) {
  private val buf = new RingArray[String](size.height)
  // Note - Lines are auto truncated...
  def appendLine(logLine: String): Unit = {
    val ansi = new AnsiString(logLine)
    // TODO - fill line to end of widget...
      val realLine =
      if(ansi.length < size.width) {
        val spaces = Seq.fill(size.width - ansi.length)(" ").mkString("")
        logLine + spaces
      } else logLine

    // TODO - figure out an accurate length on the terminal, and add spaces as needed.
    buf.append(realLine)
  }

  protected def content: String = {
    val sb = new StringBuilder()
    val itr = buf.iterator
    var line = pos.row
    while(itr.hasNext) {
      sb.append(s"${Ansi.MOVE_CURSOR(line, pos.col)}${itr.next}")
      line += 1
    }
    sb.toString
  }
}


/**
 * Hacky implementation of a mutable ring array.
 * @param buffer  The ring array buffer
 * @param start0  THe starting location in the buffer for data
 * @param size0   The number of elements in the array.
 * @tparam T      The type of data in the array.
 */
class RingArray[T: ClassTag] private (buffer: Array[T], start0: Int = 0, size0: Int = 0) {
  private var start: Int = start0
  private var mySize: Int = size0
  /** The number of elements in the arary. */
  def length: Int = mySize
  /** Grabs the element at a given index. */
  def apply(idx: Int): T =
    if(idx >= mySize) throw new IndexOutOfBoundsException(s"$idx is outside of range (0, $mySize)")
    else buffer(realIdx(idx)).asInstanceOf[T]
  /** Appends another element into the array.  If the max size is reached, this deletes the last element in. */
  def append(el: T): this.type = {
      if(mySize < buffer.length) {
      buffer(realIdx(start + mySize)) = el
      mySize += 1
    } else {
      buffer(start) = el
      start += 1
    }
    this
  }
  /** An iterator over elements in the array. */
  def iterator: Iterator[T] = new Iterator[T] {
    private var current = 0
    private var startSize = mySize
    def hasNext: Boolean = current < startSize
    def next: T = {
      try apply(current)
      finally current += 1
    }

  }
  private def realIdx(idx: Int): Int = (start + idx) % buffer.length
  /** Helper method to construct the ring array with specific buffer size. */
  def this(n: Int) = this(new Array[T](n), 0, 0)
}
