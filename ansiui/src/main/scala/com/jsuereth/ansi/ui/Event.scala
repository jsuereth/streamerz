package com.jsuereth.ansi.ui



// TODO - Limit these events to 'raw' types where the semantic extaction is done via
//        a key map.
//    Event
//    |->  ConsoleResize
//    \->  RawKeyPress
//         |-> EscapeSequence
//         \-> KeyPress


/** Events which the EventLoop will process. */
sealed trait UILoopItem
/** An event that will get fired out of the UI event loop. */
sealed trait Event extends UILoopItem
/** An action that will get performed within the UI event loop. */
sealed trait Action extends UILoopItem

/** The cursor position being fired down. */
case class CursorPosition(row: Int, col: Int) extends Event
/** The size of the console changed. */
case class ConsoleResize(rows: Int, cols: Int) extends Event
/** Denotes that a key was pressed.  Could be a single character *or* an esacpe sequence. */
sealed trait KeyPress extends Event
/** An event when a key is pressed. */
case class Key(key: Int) extends KeyPress
/** An event when a key is pressed that pushes an escape code. */
case class EscapeCode(code: String) extends KeyPress




// TODO - These extractors aren't actually guaranteed to be accurate on all platforms, best to use
// a real KeyMap -> Action.
abstract class AnsiCode(code: String) {
  def apply(): KeyPress = EscapeCode(code)
  def unapply(e: Event): Boolean =
    e match {
      case EscapeCode(`code`) => true
      case _ => false
    }
}
object UpKey extends AnsiCode("A")
object DownKey extends AnsiCode("B")
object RightKey extends AnsiCode("C")
object LeftKey extends AnsiCode("D")
object End extends AnsiCode("F")
object Home extends AnsiCode("H")



// Note: THese are special keys as denoted in ASCI that we sometimes want to directly label.
abstract class SpecialKey(key: Int) {
  def unapply(e: Event): Boolean =
    e match {
      case Key(`key`) => true
      case _ => false
    }
  def apply(): Key = Key(key)
}
object Backspace extends SpecialKey(127)
object Enter extends SpecialKey(10)
object Tab extends SpecialKey(9)
object Space extends SpecialKey(32)
object EscapeKey extends SpecialKey(27)




/** An event that will render some text to the terminal. */
case class DisplayText(ansiString: String) extends Action


/** An event that will run a given runnable on the event thread. */
case class GenericRunnable(e: Runnable) extends Action
