package com.jsuereth.ansi.ui

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorRefFactory, ActorSystem}
import akka.stream.{Materializer, ActorMaterializerSettings}
import akka.stream.actor.{ActorSubscriberMessage, ZeroRequestStrategy, RequestStrategy, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.Ansi
import com.jsuereth.ansi.ui.frp.layout.{Visible, ConsolePosition, ConsoleLayout}
import com.jsuereth.video.VideoFrame
import org.reactivestreams.{Subscription, Subscriber, Publisher}

import scala.reactive.{Reactive, Signal}

/**
 * An FRP webcam widget.
 */
object WebcamWidget {

  def createWebcam(system: ActorSystem, layout: Signal[ConsoleLayout], executions: Reactive.Emitter[GenericRunnable]) = {
    val webcam = com.jsuereth.video.WebCam.default(system)
    createGeneric(system, layout, executions, webcam)
  }

  // TODO - allow it to configure which video.
  def createVideo(system: ActorSystem, layout: Signal[ConsoleLayout], executions: Reactive.Emitter[GenericRunnable]) = {
    def rick = com.jsuereth.video.ffmpeg.readVideoFile(new java.io.File("rick.mp4"), system, playAudio = true)
    createGeneric(system, layout, executions, rick)
  }

  def createGeneric(system: ActorSystem, layout: Signal[ConsoleLayout], executions: Reactive.Emitter[GenericRunnable], source: => Publisher[VideoFrame]) = {
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
      //request(1)
      private case class RequestNextShot()
      private case class SetVisible(v: Boolean)

      // Subscribe to the layout so that we can alter our visible flag and stop getting camera events.
      private val subscription = {
        val me = self
        layout.map(_.visisble) foreach { visible =>
          me ! SetVisible(visible.value)
        }
      }

      private var lastTick = 0L
      private var last = System.currentTimeMillis()

      override def receive: Receive = {
        case SetVisible(value) =>
          if(isVisble != value) {
            if(!isVisble) request(1) // Start asking for more info
            isVisble = value
          }
        case ActorSubscriberMessage.OnNext(VideoFrame(image, timeStamp, timeUnit)) =>
          throttleToRealtime(timeStamp, timeUnit)
          val currentLayout = layout()
          if(currentLayout.visisble.value) {
            val me = self
            val renderText = {
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


      // TODO - put this somewhere more useful.
      private def throttleToRealtime(frameTime: Long, tickUnit: TimeUnit) = {
        // Here is some gunk to slow down rendering to the appropriate frame rate.
        concurrent.blocking {
          val tick = TimeUnit.NANOSECONDS.convert(frameTime, tickUnit)
          val time = System.nanoTime()
          // Ensure we aren't just starting off.
          if((lastTick > 0L) && (lastTick < tick)) {
            val diff = tick - lastTick
            val endWait = last + diff
            // TODO - we busy spin for now, forcing the thread somewhere else
            // Maybe not the best way to do this.
            while(System.nanoTime < endWait) {
              val remaining = endWait - System.nanoTime
              // TODO - we should measure the cost of each of these strategies instead of making it up on the fly.
              val remainingMS = remaining / 1000000
              if(remainingMS > 2) Thread.sleep(remainingMS, (remaining % 1000000).toInt)
              else if (remaining > 1000) Thread.`yield`()
              else ()  // Busy loop
            }
          }
          lastTick = tick
          last = System.nanoTime
        }
      }

    }
    implicit val factory = system
    val settings = ActorMaterializerSettings.create(system)
    val asciiRenderer = TerminalRenderActor.consumer(system)

    source subscribe asciiRenderer
  }
}


