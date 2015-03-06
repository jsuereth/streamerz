package com.jsuereth.ansi.ui.frp.layout

/**
 * Whitespace padding helper methods.
 */
object Padding {
  def padLines(lines: Int, cols: Int): Seq[String] =
    if(lines > 0) Seq.fill(lines)(pad(cols))
    else Seq()
  def pad(n: Int): String =
    if(n < 1) ""
    else Seq.fill(n)(' ').mkString("")
}
