package com.jsuereth.ansi.ui

import akka.actor.{ActorRefFactory, Props, ActorSystem}
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.actor._
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.ui.frp.FrpConsoleUI
import com.jsuereth.ansi.{AnsiTerminal, Ansi}
import com.jsuereth.image.Ascii
import com.jsuereth.video.{VideoFrame, AsciiVideoFrame}
import org.fusesource.jansi.{AnsiString}
import org.reactivestreams.Subscriber

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
    val x = (size.cols - text.length)-1
    val y = size.rows - 1
    s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(y,x)}$text${Ansi.RESTORE_CURSOR_POSITION}"
    //s"$text"
  }
  private val sub2 = sizeText foreach { s =>
    frp.renders += DisplayText(s)
  }

  // Layout related items
  val webcamSize = frp.consoleSize map { r =>
    ConsoleSize(r.cols/3, r.rows - (r.rows/3))
  }
  val webcamLocation = frp.consoleSize map { r =>
    ConsolePosition(0, r.cols - (r.cols/3))
  }

  val slideControl = frp.events.collect {
    case LeftKey() => PreviousSlide()
    case RightKey() => NextSlide()
    case UpKey() => FirstSlide()
    case DownKey() => LastSlide()
  }
  val slides = new SlideWidget(frp.renders, slideControl)


  // Render latest event on the screen.
  private val sub3 = latestEvent foreach { key =>
    val size = frp.consoleSize()
    val y = size.rows-2
    val text = key.toString
    val x = size.cols - (text.length + 2)
    // TODO - minimum size and fill w/ spaces to avoid having to re-redner.
    frp.renders += DisplayText(s"${Ansi.SAVE_CURSOR_POSITION}${Ansi.MOVE_CURSOR(y, x)}  $text${Ansi.RESTORE_CURSOR_POSITION}")
  }


  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("webcam-ascii-snap")
    // TODO - should we inline the webcam size/location?
    val webcam = WebcamWidget.create(system, webcamSize, webcamLocation, frp.runnables)
    val settings = MaterializerSettings.create(system)
    frp.run()
  }









}
