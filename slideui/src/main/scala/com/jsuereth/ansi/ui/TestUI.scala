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
    ConsoleSize(r.cols/4, r.rows/2)
  }
  val webcamLocation = frp.consoleSize map { r =>
    ConsolePosition(0, r.cols - (r.cols/4))
  }

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
    val webcam = com.jsuereth.video.WebCam.default(system)
    val settings = MaterializerSettings.create(system)
    // TODO - Specify camera height/width.
    // TODO - we need someway of enforcing backpressure on this stream.
    val asciiRenderer = TerminalRenderActor.consumer(system)
    webcam subscribe asciiRenderer
    Source(webcam).runWith(Sink(asciiRenderer))(FlowMaterializer(settings))
    frp.run()
  }






  // This guy is pretty hacky, but so far it is accurately displaying the webcam in the top-right corner of the screen.
  object TerminalRenderActor {
    def consumer(factory: ActorRefFactory): Subscriber[VideoFrame] = {
      val actor = factory.actorOf(Props(new TerminalRenderActor()))
      ActorSubscriber(actor)
    }
  }
  class TerminalRenderActor() extends ActorSubscriber {
    override protected def requestStrategy: RequestStrategy = ZeroRequestStrategy

    // Prime the pump.
    request(1)
    private case class RequestNextShot()

    override def receive: Receive = {
      case ActorSubscriberMessage.OnNext(VideoFrame(image, _, _)) =>
        val me = self
        val renderText = {
          val currentSize = webcamSize()
          val resized = com.jsuereth.image.Resizer.forcedScale(image, currentSize.width, currentSize.height)
          val ascii = com.jsuereth.image.Ascii.toCharacterColoredAscii(resized)
          val ConsolePosition(row, col) = webcamLocation()
          // Here we render and the fire the next video request.
          val lines =
            for((line, idx) <- ascii.split("[\r\n]+").zipWithIndex) yield {
              val y = row + idx
              // TODO - Move the camera to the far right of the screen....
              s"${Ansi.MOVE_CURSOR(y, col)}$line"
            }
          s"${Ansi.SAVE_CURSOR_POSITION}${lines.mkString("")}${Ansi.RESET_COLOR}${Ansi.RESTORE_CURSOR_POSITION}"
        }
        object FireNextRequestRunnable extends Runnable {
          def run(): Unit = {
            // TODO - Don't do it this way....
            // Here we hackily dump right to System.out since we know we're running inside a runnable on the event loop, and it's safe to do so.
            System.out.print(renderText)
            // Tell the actor to request another camera shot.
            me ! RequestNextShot()
          }
        }
        frp.runnables += GenericRunnable(FireNextRequestRunnable)

      case ActorSubscriberMessage.OnComplete =>
      case ActorSubscriberMessage.OnError(e) =>
      case RequestNextShot() => request(1)
    }

  }


}
