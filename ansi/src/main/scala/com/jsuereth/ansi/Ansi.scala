package com.jsuereth.ansi

import java.awt.Color

/**
 * A set of ANSI control codes randomnly sampled from:
 * http://en.wikipedia.org/wiki/ANSI_escape_code
 */
object Ansi {
  // Control sequence introducer for ANSI commands.
  private val CSI = s"${27.toChar}["
  // Select graphic Rendition
  private def SGR(n: Int) = s"${CSI}${n}m"

  /** Moves the current cursor to the upper left of the terminal. */
  val MOVE_CURSOR_TO_UPPER_LEFT = s"${CSI}H"

  /** Moves the cusror up, N rows */
  def MOVE_CURSOR_UP(n: Int) = s"${CSI}${n}A"
  /** Moves the cursor down N roows. */
  def MOVE_CURSOR_DOWN(n: Int) = s"${CSI}${n}B"
  /** Moves the cursor forward N cols. */
  def MOVE_CURSOR_FORWARD(n: Int) = s"${CSI}${n}C"
  /** Moves the cursor backward N cols. */
  def MOVE_CURSOR_BACK(n: Int) = s"${CSI}${n}D"
  /** Move the cursor to a specific row/col (the values here are 1-based). */
  def MOVE_CURSOR(row: Int, col: Int) = s"${CSI}${row};${col}H"
  /** Saves where the cursor is. */
  val SAVE_CURSOR_POSITION = s"${CSI}s"
  /** Restores the cursor to the last save point. */
  val RESTORE_CURSOR_POSITION = s"${CSI}u"

  /** Clears the screen from the cursor to the end of line. */
  val CLEAR_SCREEN_FROM_CURSOR_TO_END_OF_LINE = s"${CSI}0J"
  /** Clears the screen form the cursor to the beginning. */
  val CLEAR_SCREEN_FROM_CURSOR_TO_BEGINNING = s"${CSI}1J"
  /** Clears the current screen. */
  val CLEAR_SCREEN = s"${CSI}2J"



  /** Resets the current color */
  val RESET_COLOR = SGR(0)
  /** Sets the text to be bold */
  val BOLD = SGR(1)
  /** Sets the text to be underlined. */
  val UNDERLINE = SGR(4)
  /** Sets italic fonts */
  val ITALIC = SGR(3)
  /** Sets to the default font. */
  val DEFAULT_FONT = SGR(10)
  /** Changes the font */
  def SELECT_FONT(n: Int) = SGR(10 + n) // TODO - force n > 0 and n < 10

  // Note: We're trying to compress into the integer color table rather than passing rgb values,
  // as we think this is faster via "felt faster" data.
  private val ANSI_FOREGROUND = "38"
  private val ANSI_BASIC_BASE = 16
  private def toAnsiColorCode(c: Color): Int = {
    val r = toAnsiiSpace(c.getRed)
    val g = toAnsiiSpace(c.getGreen)
    val b = toAnsiiSpace(c.getBlue)
    // TODO - Check the code is valid.
    val code = ANSI_BASIC_BASE + ((r * 36) + (g * 6) + (b))
    if(code < ANSI_BASIC_BASE) ANSI_BASIC_BASE
    // TODO - what's the max?
    else if(code > 255) 255
    else code
  }
  /** Convert a color into the closest possible ANSI equivalent. */
  def FOREGROUND_COLOR(c: Color): String = {
    s"${CSI}${ANSI_FOREGROUND};5;${toAnsiColorCode(c)}m"
  }

  private val ANSI_BACKGROUND = "48"
  def BACKGROUND_COLOR(c: Color): String = {
    s"${CSI}${ANSI_BACKGROUND};5;${toAnsiColorCode(c)}m"
  }
  // Convert a magnitude in RGB 32bit space into RGB ANSI space
  private def toAnsiiSpace(mag: Int): Int = {
    // ANSII uses 6 levels of precision
    // RGB in Java starts w/ 255 precision
    (6 * (mag.toFloat / 255)).toInt
  }

  // remaining colors
  def ANSI_COLOR_TABLE_FOREGROUND(idx: Int): String = SGR(30 + idx)
  def ANSI_COLOR_TABLE_BACKGROUND(idx: Int): String = SGR(40 + idx)


  val BLACK = ANSI_COLOR_TABLE_FOREGROUND(0)
  val RED = ANSI_COLOR_TABLE_FOREGROUND(1)
  val GREEN = ANSI_COLOR_TABLE_FOREGROUND(2)
  val YELLOW = ANSI_COLOR_TABLE_FOREGROUND(3)
  val BLUE = ANSI_COLOR_TABLE_FOREGROUND(4)
  val MAGENTA = ANSI_COLOR_TABLE_FOREGROUND(5)
  val CYAN = ANSI_COLOR_TABLE_FOREGROUND(6)
  val WHITE = ANSI_COLOR_TABLE_FOREGROUND(7)

  val BLACK_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(0)
  val RED_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(1)
  val GREEN_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(2)
  val YELLOW_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(3)
  val BLUE_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(4)
  val MAGENTA_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(5)
  val CYAN_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(6)
  val WHITE_BACKGROUND = ANSI_COLOR_TABLE_BACKGROUND(7)
}