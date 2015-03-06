package com.jsuereth.ansi.ui

import akka.actor.{ActorRefFactory, Props, ActorSystem}
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.actor._
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.ui.frp.FrpConsoleUI
import com.jsuereth.ansi.ui.frp.layout._
import com.jsuereth.ansi.{AnsiTerminal, Ansi}
import com.jsuereth.image.Ascii
import com.jsuereth.video.{VideoFrame, AsciiVideoFrame}
import org.fusesource.jansi.{AnsiString}
import org.reactivestreams.Subscriber

import scala.reactive.Signal

/**
 * Created by jsuereth on 3/1/15.
 */
object TestUI {


  val frp = new FrpConsoleUI()

  // Handle backspace
  private val sub1 = frp.events foreach {
    case Backspace() =>
      frp.renders += DisplayText(s"${Ansi.MOVE_CURSOR_BACK(1)} ${Ansi.MOVE_CURSOR_BACK(1)}")
    case _ =>
      // Ignore
  }

  private val latestEvent = frp.events.signal(KeyPress(0))
  // TODO - This may be too aggressive...
  private val subResize = frp.consoleSize foreach { change =>
    frp.renders += DisplayText(Ansi.CLEAR_SCREEN)}

  // Our current text to display as size.
  val sizeText = frp.consoleSize map { size =>
    val text = s"${size.cols} x ${size.rows}"
    val x = (size.cols - 20)
    val y = size.rows - 1
    val pad = Padding.pad(20-text.length)
    s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(y,x)}$pad$text${Ansi.RESTORE_CURSOR_POSITION}"
    //s"$text"
  }
  private val sub2 = sizeText foreach { s =>
    frp.renders += DisplayText(s)
  }


  // TODO - this isn't very good

  lazy val webcamVisible: Signal[VisibleFlag] = {
    val keyPress = frp.events.collect {
      case KeyPress('c') => Visible
      case KeyPress('C') => NotVisible
    }
    keyPress.signal(Visible)
  }
  val completeLayout: Signal[SlideUILayout] =
    (frp.consoleSize zip webcamVisible) { (s, wcv) =>
      val startingLayout = ConsoleLayout(ConsolePosition(1,1), ConsoleSize(width = s.cols, height = s.rows-3), Visible)
      System.err.println(s"Console Resize: $startingLayout")
      val layout =
      if(wcv.value) {
        val (slides, right) = Layouts.horizontalSplit(startingLayout, 0.7f)
        val (camera, ignore) = Layouts.verticalSplit(right)
        SlideUILayout(camera, slides)
      } else SlideUILayout(ConsoleLayout.empty, startingLayout)
      System.err.println(layout)
      layout
    }

  val webcamLayout = completeLayout.map(_.camera)

  val slideControl = frp.events.collect {
    case LeftKey() => PreviousSlide()
    case RightKey() => NextSlide()
    case UpKey() => FirstSlide()
    case DownKey() => LastSlide()
  }
  val slideLayout = completeLayout.map(_.slides)
  val slides = new SlideWidget(frp.renders, slideControl, slideLayout)


  // Render latest event on the screen.
  private val sub3 = latestEvent foreach { key =>
    val size = frp.consoleSize()
    val y = size.rows-2
    val text = key.toString.take(25)
    val x = size.cols - 25
    val pad = Padding.pad(25-text.length)
    // TODO - minimum size and fill w/ spaces to avoid having to re-redner.
    frp.renders += DisplayText(s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(y, x)}$pad$text${Ansi.RESTORE_CURSOR_POSITION}")
  }


  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("webcam-ascii-snap")
    // TODO - should we inline the webcam size/location?
    val webcam = WebcamWidget.create(system, webcamLayout, frp.runnables)
    val settings = MaterializerSettings.create(system)
    frp.run()
  }
}
// TODO - define all widgets we render.
case class SlideUILayout(
  camera: ConsoleLayout,
  slides: ConsoleLayout
) {
  override def toString =
    s"""Layout {
       |  camera: ${camera}
       |  slides: ${slides}
       |}""".stripMargin
}
