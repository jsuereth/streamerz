package com.jsuereth.ansi.ui

/** Something which can dispatch events. */
trait EventDispatcher {
  /** handle an event, globally, using this event dispatcher. */
  def dispatch(e: Event): Unit
}
