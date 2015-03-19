package com.jsuereth

import com.jsuereth.ansi.Ansi
import com.jsuereth.ansi.ui.{DisplayText, KeyPress, DownKey, UpKey}
import com.jsuereth.ansi.ui.frp.FrpConsoleUI

import scala.reactive.{Signal, Reactive}

/**
 * Created by jsuereth on 3/14/15.
 */
object Pong {

  val frp = new FrpConsoleUI()

  val clear =
    frp.consoleSize.changes foreach { x =>
      frp.renders += DisplayText(Ansi.CLEAR_SCREEN)
    }

  val rightPaddleControl =
    frp.events.collect {
      case UpKey() => Up
      case DownKey() => Down
    }

  val leftPaddleControl =
    frp.events.collect {
      case KeyPress(x) if x == 'a'.toInt => Up
      case KeyPress(x) if x == 'z'.toInt => Down
    }

  def paddleState(control: Reactive[PaddleControl], isLeft: Boolean): Signal[PaddleState] = {
    lazy val result: Signal[PaddleState] = {
      control.map {
        case Up =>
          val current = result()
          val nextHeight = current.y - 1
          val h =
            if(nextHeight - current.width < 1) 1
            else nextHeight
          current.copy(y = h)
        case Down =>
          val current = result()
          val height = frp.consoleSize().rows
          val nextHeight = current.y + 1
          val h =
            if(nextHeight + current.width > height) height
            else nextHeight
          current.copy(y = h)
      }.signal(PaddleState(3, 10, 4))
    }
    result.zip(frp.consoleSize) { (state, size) =>
      if(!isLeft) state.copy(x = size.cols-3)
      else state
    }
  }

  val leftPaddleState: Signal[PaddleState] = paddleState(leftPaddleControl, true)
  val rightPaddleState: Signal[PaddleState] = paddleState(rightPaddleControl, false)

  def renderPaddle(paddleState: Signal[PaddleState], color: String) = {
    paddleState.changes foreach { state =>
      val lines =
        for (row <- 1 to frp.consoleSize().rows) yield {
          if ((row > (state.y - (state.width))) &&  (row < (state.y + (state.width)))) s"${Ansi.MOVE_CURSOR(row, state.x)}${color}#"
          else s"${Ansi.MOVE_CURSOR(row, state.x)} "
        }
      frp.renders += DisplayText(lines.mkString(s"${Ansi.SAVE_CURSOR_POSITION}", "", s"${Ansi.RESTORE_CURSOR_POSITION}"))
      state.y
    }
  }
  val leftPaddleRender = renderPaddle(leftPaddleState, Ansi.BLUE)
  val rightPaddleRender = renderPaddle(rightPaddleState, Ansi.GREEN)




  def main(args: Array[String]): Unit = {
    frp.run()
  }

}


sealed trait PaddleControl
case object Up extends PaddleControl
case object Down extends  PaddleControl


case class PaddleState(
  x: Int,
  y: Int,
  width: Int
)