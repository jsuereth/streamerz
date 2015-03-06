package com.jsuereth.ansi.ui.frp.layout

/**
 * Computational helpers for layouts.
 */
object Layouts {


  /**
   * Splits a single layout into two layouts (verticle) with the given ratio for layout of left to right..
   *
   * @return  Pair of (left,right) layouts.
   */
  def horizontalSplit(layout: ConsoleLayout, ratio: Float = 0.5f): (ConsoleLayout, ConsoleLayout) = {
    val width = layout.size.width
    val leftWidth = (width * ratio + 0.5f).toInt
    val rightWidth = width - leftWidth
    val leftLayout = ConsoleLayout(
      layout.pos,
      layout.size.copy(width = leftWidth),
      layout.visisble
    )
    val rightLayout = ConsoleLayout(
      layout.pos.copy(col = layout.pos.col + leftWidth),
      layout.size.copy(width = rightWidth),
      layout.visisble
    )
    (leftLayout, rightLayout)
  }

  /**
   * Splits a single layout into two layouts (horizontally) with the given ratio for layout top to bottom
   * @return  A pair of (top, bottom) layouts
   */
  def verticalSplit(layout: ConsoleLayout, ratio: Float = 0.5f): (ConsoleLayout, ConsoleLayout) = {
    val height = layout.size.height
    val topHeight = ((height * ratio) + 0.5f).toInt
    val rightHeight = height - topHeight

    val topLayout = ConsoleLayout(
      layout.pos,
      layout.size.copy(height = topHeight),
      layout.visisble
    )
    val bottomLayout = ConsoleLayout(
      layout.pos.copy(row = layout.pos.row + topHeight),
      layout.size.copy(height = rightHeight),
      layout.visisble
    )
    (topLayout, bottomLayout)
  }
}
