package com.jsuereth.ansi.ui

import akka.actor.{ActorRefFactory, Props, ActorSystem}
import akka.stream.{Materializer, ActorMaterializerSettings}
import akka.stream.actor._
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.ui.frp.FrpConsoleUI
import com.jsuereth.ansi.ui.frp.layout._
import com.jsuereth.ansi.{AnsiTerminal, Ansi}
import com.jsuereth.image.Ascii
import com.jsuereth.video.{VideoFrame, AsciiVideoFrame}
import org.reactivestreams.Subscriber

import scala.reactive.Signal

/**
 * Created by jsuereth on 3/1/15.
 */
object TestUI {


  val frp = new FrpConsoleUI()

  // TODO - This may be too aggressive...
  // CLear the screen every resize event.
  val subResize = frp.consoleSize foreach { change =>
    frp.renders += DisplayText(Ansi.CLEAR_SCREEN)}


  // Add a label which displays the latest event.
  private val latestEventLabel = frp.label(
    text=frp.events.signal(KeyPress(0)).map(_.toString),
    layout=frp.consoleSize map { s => ConsoleLayout(ConsolePosition(s.rows-2, s.cols-25), ConsoleSize(25,1), Visible)})



  // Add a label which displays the current size of the terminal
  val sizeLabel = {
    val layout= frp.consoleSize map { s => ConsoleLayout(ConsolePosition(s.rows-1, s.cols-20), ConsoleSize(20, 1), Visible) }
    val text = frp.consoleSize map { s => s"${Ansi.BLUE}${s.cols} x ${s.rows}${Ansi.RESET_COLOR}"}
    frp.label(text, layout)
  }


  // TODO - avoid firing if no change.
  // TODO - possibly clear screen on state change...
  lazy val slideUiState: Signal[SlideUiState] = {
    val keyPress = frp.events.collect {
      case End() =>
        slideUiState() match {
          case FullScreenCamera => SlidesAndCamera
          case FullScreenSlides => FullScreenSlides
          case SlidesAndCamera => FullScreenSlides
          case RickRoll => FullScreenSlides
        }
      case Home() =>
        slideUiState() match {
          case RickRoll => FullScreenCamera
          case FullScreenCamera => FullScreenCamera
          case SlidesAndCamera => FullScreenCamera
          case FullScreenSlides => SlidesAndCamera
        }
      case KeyPress(114) => RickRoll
    }
    keyPress.signal(FullScreenSlides)
  }

  val completeLayout: Signal[SlideUILayout] =
    (frp.consoleSize zip slideUiState) { (s, wcv) =>
      val startingLayout = ConsoleLayout(ConsolePosition(1,1), ConsoleSize(width = s.cols, height = s.rows-3), Visible)
      System.err.println(s"Console Resize: $startingLayout")
      val layout =
        slideUiState() match {
          case SlidesAndCamera =>
            val (slides, right) = Layouts.horizontalSplit(startingLayout, 0.7f)
            val (camera, ignore) = Layouts.verticalSplit(right)
            SlideUILayout(camera, slides, blank = ignore)
          case FullScreenCamera =>
            SlideUILayout(startingLayout, ConsoleLayout.empty)
          case FullScreenSlides =>
            SlideUILayout(ConsoleLayout.empty, startingLayout)
          case RickRoll =>
            SlideUILayout(ConsoleLayout.empty, ConsoleLayout.empty, startingLayout)
        }
      System.err.println(layout)
      layout
    }

  val webcamLayout = completeLayout.map(_.camera)
  val rickLayout = completeLayout.map(_.rick)

  val slideControl = frp.events.collect {
    case Space() => NextSlide()
    case LeftKey() => PreviousSlide()
    case RightKey() => NextSlide()
    case UpKey() => FirstSlide()
    case DownKey() => LastSlide()
  }
  val slideLayout = completeLayout.map(_.slides)
  val slides = new SlideWidget(frp.renders, slideControl, slideLayout)



  private val blankLabel = slideControl.map(_ => " ").signal(" ")
  private val blank = frp.label(
    blankLabel,
    frp.consoleSize.map{ s => ConsoleLayout(ConsolePosition(s.rows-2, 0), ConsoleSize(s.cols-25, 3), Visible)}
  )

  private val blank2 = frp.label(
    blankLabel,
    completeLayout.map(_.blank)
  )

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("webcam-ascii-snap")
    // TODO - should we inline the webcam size/location?
    val webcam = WebcamWidget.createWebcam(system, webcamLayout, frp.runnables)
    // TODO sync rick to realtime.
    val rick = WebcamWidget.createVideo(system, rickLayout, frp.runnables)
    val settings = ActorMaterializerSettings.create(system)
    frp.run()
  }
}
// TODO - define all widgets we render.
case class SlideUILayout(
  camera: ConsoleLayout,
  slides: ConsoleLayout,
  rick: ConsoleLayout = ConsoleLayout.empty,
  blank: ConsoleLayout = ConsoleLayout.empty
) {
  override def toString =
    s"""Layout {
       |  camera: ${camera}
       |  slides: ${slides}
       |  rick: ${rick}
       |}""".stripMargin
}
sealed trait SlideUiState
case object FullScreenCamera extends SlideUiState
case object FullScreenSlides extends SlideUiState
case object SlidesAndCamera extends SlideUiState
case object RickRoll extends SlideUiState
