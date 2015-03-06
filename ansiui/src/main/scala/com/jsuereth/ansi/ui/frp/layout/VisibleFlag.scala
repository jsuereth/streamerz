package com.jsuereth.ansi.ui.frp.layout


// Layout related items.
sealed trait VisibleFlag {
  def value: Boolean
}
case object Visible extends VisibleFlag{
  def value: Boolean = true
  def not: VisibleFlag = NotVisible
}
case object NotVisible extends VisibleFlag {
  def value: Boolean = false
  def not: VisibleFlag = Visible
}


