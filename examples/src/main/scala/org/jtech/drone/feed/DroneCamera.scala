package org.jtech.drone.feed

import java.awt.{Color, Graphics2D}
import java.awt.image.BufferedImage

import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.actor.{Props, ActorSystem, ActorRefFactory}
import com.codeminders.ardrone.{DroneVideoListener, DroneStatusChangeListener, ARDrone}
import com.typesafe.scalalogging.LazyLogging
import org.reactivestreams.Publisher

object DroneCamera {

  def default(system: ActorRefFactory): Publisher[BufferedImage] =
    cameraStream(system)
  
  private def cameraStream(system: ActorRefFactory): Publisher[BufferedImage] =
    ActorPublisher(system.actorOf(DroneCameraProducer.props))
}

object DroneCameraProducer {
  def props: Props = Props(new DroneCameraProducer())
}


private class DroneCameraProducer extends ActorPublisher[BufferedImage] with LazyLogging with DroneVideoListener {

  val drone = new ARDrone()
  var connected = false
  var image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB)

  {
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

    g2d.dispose()
  }

  def initDrone() = {
    drone.addStatusChangeListener(new DroneStatusChangeListener() {
      override def ready() {
        logger.debug("Drone ready")
      }
    })
//    drone.selectVideoChannel(ARDrone.VideoChannel.HORIZONTAL_ONLY)
    drone.enableAutomaticVideoBitrate()
    drone.connect()
    drone.waitForReady(10000)
    drone.clearEmergencySignal()
    drone.addImageListener(this)
  }
  override def receive: Receive = {
    case b: BufferedImage =>
      image = b
    case ActorPublisherMessage.Request(elements) =>
      while (totalDemand > 0) onNext(getFrame)
    case ActorPublisherMessage.Cancel =>
      drone.disconnect()
      context stop self
  }

  def getFrame: BufferedImage = {
//    if (!connected) initDrone()
    image
  }

  override def frameReceived(startX: Int, startY: Int,
                             w: Int, h: Int,
                             rgbArray: Array[Int],
                             offset: Int, scanSize: Int): Unit = {
    val image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    image.setRGB(startX, startY, w, h, rgbArray, offset, scanSize)
    self ! image
  }
}

