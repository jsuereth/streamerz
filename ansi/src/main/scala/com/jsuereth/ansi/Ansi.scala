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
  /** Clears the current screen. */
  val CLEAR_SCREEN = s"${CSI}2J"
  /** Resets the current color */
  val RESET_COLOR = SGR(0)
  Console.RESET
  /** Sets the text to be bold */
  val BOLD = SGR(1)
  /** Sets the text to be underlined. */
  val UNDERLINE = SGR(2)
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
}