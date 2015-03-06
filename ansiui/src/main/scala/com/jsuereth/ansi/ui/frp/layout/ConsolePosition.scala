package com.jsuereth.ansi.ui.frp.layout

/** The position for a widget. */
case class ConsolePosition(row: Int, col: Int) {
  override def toString = s"(row=$row, col=$col)"
}
