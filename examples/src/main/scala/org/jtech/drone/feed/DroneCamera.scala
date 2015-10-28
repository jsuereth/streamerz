package org.jtech.drone.feed

import java.awt.image.BufferedImage
import java.awt.{Font, Color, Graphics2D}

import akka.actor.{ActorRefFactory, Props}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import com.typesafe.scalalogging.LazyLogging
import de.yadrone.base.ARDrone
import de.yadrone.base.command.VideoCodec
import de.yadrone.base.video.ImageListener
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object DroneCamera {

  def default(system: ActorRefFactory): Publisher[BufferedImage] =
    cameraStream(system)
  
  private def cameraStream(system: ActorRefFactory): Publisher[BufferedImage] =
    ActorPublisher(system.actorOf(DroneCameraProducer.props))
}

object DroneCameraProducer {
  def props: Props = Props(new DroneCameraProducer())
}


private class DroneCameraProducer extends ActorPublisher[BufferedImage] with LazyLogging with ImageListener {

  val drone = new ARDrone()
  var connected = false
  var image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB)
  initImage()

  def initImage() {
    val w1 = image.getWidth / 7
    val w2 = image.getWidth / 6
    val h2 = image.getHeight / 7
    val h3 = image.getHeight / 4
    val h1 = image.getHeight - h2 - h3
    val g2d = image.getGraphics.asInstanceOf[Graphics2D]

    val colors1 = List(Color.WHITE, Color.YELLOW, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.BLUE)
    val colors2 = List(Color.BLUE, Color.DARK_GRAY, Color.MAGENTA, Color.GRAY, Color.CYAN, Color.DARK_GRAY, Color.WHITE)
    val colors3 = List(new Color(0, 100, 100), Color.WHITE, Color.BLUE.darker(), Color.GRAY, Color.LIGHT_GRAY, Color.DARK_GRAY)

    colors1.zipWithIndex.foreach { case (color, i) =>
      g2d.setColor(color)
      g2d.fillRect(i * w1, 0, w1, h1)
    }
    colors2.zipWithIndex.foreach { case (color, i) =>
      g2d.setColor(color)
      g2d.fillRect(i * w1, h1, w1, h2)
    }
    colors3.zipWithIndex.foreach { case (color, i) =>
      g2d.setColor(color)
      g2d.fillRect(i * w2, h1 + h2, w2, h3)
    }

    g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 74))
    g2d.drawString("Booting", 0, image.getHeight / 2)

    g2d.dispose()
  }

  def initDrone() = {
    drone.start()
    drone.getCommandManager.setVideoCodec(VideoCodec.H264_AUTO_RESIZE)
    drone.getCommandManager.setMaxVideoBitrate(250)
    drone.getCommandManager.setVideoCodecFps(30)
    drone.setHorizontalCamera()
    drone.getVideoManager.addImageListener(this)
    connected = true
  }
  override def receive: Receive = {
    case bi: BufferedImage =>
      if (totalDemand > 0) onNext(bi)
    case ActorPublisherMessage.Request(elements) =>
      if (!connected & totalDemand > 0) onNext(image)
    case ActorPublisherMessage.Cancel =>
      drone.getVideoManager.removeImageListener(this)
      drone.stop()
      connected = false
      context stop self
  }

  Future { initDrone() }

  override def imageUpdated(bi: BufferedImage): Unit = {
    self ! bi
  }

}

