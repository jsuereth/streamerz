package com.jsuereth.ansi.ui

import java.awt.Color

import com.jsuereth.ansi.Ansi
import org.fusesource.jansi.AnsiString

import scala.reflect.ClassTag

case class ConsolePosition(row: Int, col: Int)
case class ConsoleSize(width: Int, height: Int)

/**
 * A widget that renders via ANSI codes to a location on the string.
 */
abstract class Widget(pos: ConsolePosition) {
  protected def content: String
  def renderString: String =
    s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(pos.row, pos.col)}$content${Ansi.RESTORE_CURSOR_POSITION}"
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


// Hacky implementation of a ring array
class RingArray[T: ClassTag] private (buffer: Array[T], start0: Int = 0, size0: Int = 0) {
  private var start: Int = start0
  private var mySize: Int = size0
  def length: Int = mySize
  def apply(idx: Int): T =
    if(idx >= mySize) throw new IndexOutOfBoundsException(s"$idx is outside of range (0, $mySize)")
    else buffer(realIdx(idx)).asInstanceOf[T]
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
  /** Helper method to construct the ring array froma  size. */
  def this(n: Int) = this(new Array[T](n), 0, 0)
}
