package com.jsuereth.ansi

import java.io.InputStreamReader


case class CursorPosition(row: Int, col: Int)

/**
 * Hackery to help discover things about an ANSI terminal
 */
object AnsiTerminal {

  private val ABSURD_CONSOLE_SIZE = 50000
  /**
   * A call which attempts to grab the current ANSI terminal's size.
   *
   * This call will attempt to issue an ANSI escape via System.out, and then read the response on System.in.  As
   * may be observed, this could be horribly broken if there are any pending values on the input channel.  As such,
   * this method may need some "tuning" in the future.
   *
   * The ANSI escape we use is a hack to move th cursor as far to the bottom right of the screen as any reasonable terminal
   * would support, then have it report the actual cursor position, before going back to where we were.
   *
   * @return
   *         The current terminal's size, Rows x Columns.
   */
  def getTerminalSize: (Int, Int) = {
    System.out.println(s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(ABSURD_CONSOLE_SIZE,ABSURD_CONSOLE_SIZE)}${Ansi.REPORT_CURSOR_POSITION}${Ansi.RESTORE_CURSOR_POSITION}")
    val CursorPosition(rows,cols) = readCursorPosition()
    (rows,cols)
  }


  /**
   * A call which can read the cursor position escape code off of standard input.
   */
  def readCursorPosition(reader: InputStreamReader = new InputStreamReader(System.in)): CursorPosition = {
    //System.out.println(s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(50000,50000)}${Ansi.REPORT_CURSOR_POSITION}${Ansi.RESTORE_CURSOR_POSITION}")
    // Here we try to read EXACTLY the number of characters we need for the escape code.
    // ESC[n;mR
    def parseCSI(): Unit = {
      reader.read.toChar match {
        case 27 => ()
        case c => throw new IllegalStateException(s"Expected to find ANSI escape sequence, instead found: $c")
      }
      reader.read.toChar match {
        case '[' => ()
        case c => throw new IllegalArgumentException(s"Expected to find ANSI escape sequence, instead found: $c")
      }
    }
    // Here we read the cursor position row
    def readNumLines(sb: StringBuffer = new StringBuffer): String =
      reader.read.toChar match {
        case ';' => sb.toString
        case n => sb.append(n); readNumLines(sb) // TODO - force this to be a legit digit.
      }
    // Here we read the cursors position column.
    def readNumColumns(sb: StringBuffer = new StringBuffer()): String =
      reader.read.toChar match {
        case 'R' => sb.toString
        case n => sb.append(n); readNumColumns(sb) // TODO - force this to be a legit digit.
      }

    parseCSI()
    val lines = readNumLines()
    val cols = readNumColumns()
    CursorPosition(lines.toInt, cols.toInt)
  }
}
