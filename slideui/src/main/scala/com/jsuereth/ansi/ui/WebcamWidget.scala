package com.jsuereth.ansi.ui

import akka.actor.{Props, ActorRefFactory, ActorSystem}
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.actor.{ActorSubscriberMessage, ZeroRequestStrategy, RequestStrategy, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.Ansi
import com.jsuereth.ansi.ui.frp.layout.{ConsolePosition, ConsoleLayout}
import com.jsuereth.video.VideoFrame
import org.reactivestreams.Subscriber

import scala.reactive.{Reactive, Signal}

/**
 * An FRP webcam widget.
 */
object WebcamWidget {
  def create(system: ActorSystem, layout: Signal[ConsoleLayout], executions: Reactive.Emitter[GenericRunnable]) = {
    // This guy is pretty hacky, but so far it is accurately displaying the webcam in the top-right corner of the screen.
    object TerminalRenderActor {
      def consumer(factory: ActorRefFactory): Subscriber[VideoFrame] = {
        val actor = factory.actorOf(Props(new TerminalRenderActor()))
        ActorSubscriber(actor)
      }
    }
    class TerminalRenderActor() extends ActorSubscriber {
      override protected def requestStrategy: RequestStrategy = ZeroRequestStrategy
      private var isVisble: Boolean = false
      // Prime the pump.
      request(1)
      private case class RequestNextShot()
      private case class SetVisible(v: Boolean)

      // Subscribe to the layout so that we can alter our visible flag and stop getting camera events.
      private val subscription = {
        val me = self
        layout.map(_.visisble) foreach { visible =>
          me ! SetVisible(visible.value)
        }
      }


      override def receive: Receive = {
        case SetVisible(value) =>
          if(isVisble != value) {
            if(!isVisble) request(1) // Start asking for more info
            isVisble = value
          }
        case ActorSubscriberMessage.OnNext(VideoFrame(image, _, _)) =>
          if(isVisble) {
            val me = self
            val renderText = {
              val currentLayout = layout()
              val currentSize = currentLayout.size
              val resized = com.jsuereth.image.Resizer.forcedScale(image, currentSize.width, currentSize.height)
              val ascii = com.jsuereth.image.Ascii.toCharacterColoredAscii(resized)
              val ConsolePosition(row, col) = currentLayout.pos
              // Here we render and the fire the next video request.
              val lines =
                for ((line, idx) <- ascii.split("[\r\n]+").zipWithIndex) yield {
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
            executions += GenericRunnable(FireNextRequestRunnable)
          }
        case ActorSubscriberMessage.OnComplete =>
        case ActorSubscriberMessage.OnError(e) =>
        case RequestNextShot() => request(1)
      }

    }
    val webcam = com.jsuereth.video.WebCam.default(system)
    implicit val factory = system
    val settings = MaterializerSettings.create(system)
    // TODO - Specify camera height/width.
    // TODO - we need someway of enforcing backpressure on this stream.
    val asciiRenderer = TerminalRenderActor.consumer(system)
    webcam subscribe asciiRenderer
    Source(webcam).runWith(Sink(asciiRenderer))(FlowMaterializer(settings))
  }
}


