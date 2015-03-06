package com.jsuereth.ansi.ui.frp.layout

/** The size fora  widget (in console coordinates. */
case class ConsoleSize(width: Int, height: Int) {
  override def toString = s"${width}x${height}"
}
