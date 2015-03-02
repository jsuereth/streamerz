package com.jsuereth.ansi.ui

import akka.actor.{ActorRefFactory, Props, ActorSystem}
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.actor._
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.{AnsiTerminal, Ansi}
import com.jsuereth.image.Ascii
import com.jsuereth.video.AsciiVideoFrame
import org.fusesource.jansi.{AnsiString}
import org.reactivestreams.Subscriber

/**
 * Created by jsuereth on 3/1/15.
 */
object TestUI {

  object MyDispatcher extends EventDispatcher {
    /** handle an event, globally, using this event dispatcher. */
    override def dispatch(e: Event): Unit = e match {
      case Backspace() =>
        loop.fire(DisplayText(s"${Ansi.MOVE_CURSOR_BACK(1)} ${Ansi.MOVE_CURSOR_BACK(1)}"))
      case KeyPress(char) =>
        loop.fire(DisplayText(s"\rKeyPress($char)"))
      case CursorPosition(row,col) => loop.fire(DisplayText(s"${Ansi.BLUE}DEBUG${Ansi.RESET_COLOR}: Console size: $row x $col \n"))
      case e => // Ignore
        loop.fire(DisplayText(s"\r$e"))
    }
  }

  val loop = MainUILoop.apply(MyDispatcher)




  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("webcam-ascii-snap")
    val (rows, cols) = AnsiTerminal.getTerminalSize
    System.out.print(s"${Ansi.CLEAR_SCREEN}${Ansi.MOVE_CURSOR_TO_UPPER_LEFT}")
    val webcam = Source(com.jsuereth.video.WebCam.default(system))
    val settings = MaterializerSettings.create(system)
    // TODO - Specify camera height/width.
    val asciifier = com.jsuereth.video.AsciiVideo.asciiConversion(Ascii.toCharacterColoredAscii, cols/2, rows/2)
    // TODO - we need someway of enforcing backpressure on this stream.
    val asciiRenderer = TerminalRenderActor.consumer(system, 1, cols/2, cols/2)
    asciifier.to(Sink(asciiRenderer)).runWith(webcam)(FlowMaterializer(settings))
    loop.run()
  }




  // This guy is pretty hacky, but so far it is accurately displaying the webcam in the top-right corner of the screen.
  object TerminalRenderActor {
    def consumer(factory: ActorRefFactory, row: Int, col: Int, width: Int): Subscriber[AsciiVideoFrame] = {
      val actor = factory.actorOf(Props(new TerminalRenderActor(row, col, width)))
      ActorSubscriber(actor)
    }
  }
  class TerminalRenderActor(row: Int, col: Int, width: Int) extends ActorSubscriber {
    override protected def requestStrategy: RequestStrategy = ZeroRequestStrategy

    // Prime the pump.
    request(1)
    private case class RequestNextShot()

    override def receive: Receive = {
      case ActorSubscriberMessage.OnNext(AsciiVideoFrame(image, _, _)) =>
        val me = self
        object FireNextRequestRunnable extends Runnable {
          def run(): Unit = {
            // Here we render and the fire the next video request.
            val lines =
              for((line, idx) <- image.split("[\r\n]+").zipWithIndex) yield {
                val y = row + idx
                // Somewhat CPU intensive mechanism of moving the camera feed to the far right of the console.
                val length = new AnsiString(line).length()
                val x = col + (width - length)
                s"${Ansi.MOVE_CURSOR(y, x)}$line"
              }
            // Here we hackily dump right to System.out since we know we're running inside a runnable on the event loop, and it's safe to do so.
            System.out.print(s"${Ansi.SAVE_CURSOR_POSITION}${lines.mkString("")}${Ansi.RESET_COLOR}${Ansi.RESTORE_CURSOR_POSITION}")
            // Tell the actor to request another camera shot.
            me ! RequestNextShot()
          }
        }
        loop.fire(GenericRunnable(FireNextRequestRunnable))

      case ActorSubscriberMessage.OnComplete =>
      case ActorSubscriberMessage.OnError(e) =>
      case RequestNextShot() => request(1)
    }

  }


}
