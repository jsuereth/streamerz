package com.jsuereth.ansi.ui.frp.layout

/** represents the layout of a given widget within the console. */
case class ConsoleLayout(pos: ConsolePosition, size: ConsoleSize, visisble: VisibleFlag) {
  override def toString =
    if(visisble.value) s"""${pos} @ ${size}"""
    else "NotVisible"
}
object ConsoleLayout {
  def empty: ConsoleLayout = ConsoleLayout(ConsolePosition(1,1), ConsoleSize(0,0), NotVisible)
}
