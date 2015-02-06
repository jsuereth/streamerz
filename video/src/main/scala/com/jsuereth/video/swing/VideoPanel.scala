package com.jsuereth.video
package swing


import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import java.awt.Color
import java.awt.Graphics
import akka.actor.{Props, ActorRef, ActorRefFactory}
import akka.stream.actor.{ActorSubscriberMessage, OneByOneRequestStrategy, ActorSubscriber}
import org.reactivestreams.Subscriber

/**
 * A video panel which can consume Frame elements and display them in the UI
 *  at whatever rate they are fired.
 */
private[swing] class VideoPanel extends JComponent {
  private var lastFrame: Option[VideoFrame] = None
  private val color = Color.BLACK


  override protected def paintComponent(g: Graphics): Unit =
    lastFrame match {
      case None =>
        g.setColor(color)
        g.drawRect(0, 0, getWidth, getHeight)
      case Some(frame) => drawFrame(g, frame)
    }

  def updateFrame(f: VideoFrame): Unit = {
    lastFrame = Some(f)
    drawFrame(getGraphics, f)
  }

  private def drawFrame(g: Graphics, frame: VideoFrame): Unit =
    if (g != null) {
      g.drawImage(frame.image, 0, 0, getWidth, getHeight, 0, 0, frame.image.getWidth, frame.image.getHeight, color, this)
    }
}
private[swing] class VideoPanelActor(panel: VideoPanel) extends ActorSubscriber {
  override val requestStrategy = OneByOneRequestStrategy
  private var last = System.nanoTime()
  private var lastTick = 0L

  def receive: Receive = {
    case ActorSubscriberMessage.OnNext(frame: VideoFrame) =>
      // Here is some gunk to slow down rendering to the appropriate frame rate.
      concurrent.blocking {
        val tick = TimeUnit.NANOSECONDS.convert(frame.timeStamp, frame.timeUnit)
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
      panel.updateFrame(frame)
    case ActorSubscriberMessage.OnComplete =>
    // TODO - blank out the screen
    case ActorSubscriberMessage.OnError(err) =>
    // TODO - display error.
  }
}
object VideoPanel  {
  private def props(panel: VideoPanel): Props = Props(new VideoPanelActor(panel))

  def make(factory: ActorRefFactory): (ActorRef, JComponent) = {
    // TODO - this is horribly wrong for error handling, but the alternative is more annoying and
    // much harder to implement (rewiring actors to physical swing controls when restarted).
    val panel = new VideoPanel()
    val actorRef = factory.actorOf(props(panel).withDispatcher("swing-dispatcher"), "video-panel")
    (actorRef, panel)
  }

  /** Construct a video panel which consumes frames and renders them on the swing component. */
  def apply(factory: ActorRefFactory): (Subscriber[VideoFrame], JComponent) = {
    // TODO - this is horribly wrong for error handling, but the alternative is more annoying and
    // much harder to implement (rewiring actors to physical swing controls when restarted).
    val panel = new VideoPanel()
    val actorRef = factory.actorOf(props(panel).withDispatcher("swing-dispatcher"), "video-panel")
    (ActorSubscriber[VideoFrame](actorRef), panel)
  }
}