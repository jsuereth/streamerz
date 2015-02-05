package com.jsuereth.video

import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.{Webcam=>WC}
import java.util.concurrent.TimeUnit
import akka.actor.{Props, ActorSystem, ActorRefFactory}
import org.reactivestreams.Publisher
import collection.JavaConverters._

/** Helpers for creating streams of events coming out of a webcam. */
object WebCam {

  /** Create a stream of video frames from the default webcam. */
  def default(system: ActorRefFactory): Publisher[VideoFrame] =
    cameraStream(system)(WC.getDefault)

  /** Grab all the video cameras streams. */
  def cameraStreams(system: ActorSystem): Seq[Publisher[VideoFrame]] =
    WC.getWebcams.asScala map cameraStream(system)

  private def cameraStream(system: ActorRefFactory)(cam: WC): Publisher[VideoFrame] =
    ActorPublisher(system.actorOf(WebCamProducer.props(cam)))
}

/** helpers for the WebCamProducer actor. */
object WebCamProducer {
  def props(cam: WC): Props = Props(new WebCamProducer(cam))
}

/** An actor which reads the given file on demand. */
private[video] class WebCamProducer(cam: WC) extends ActorPublisher[VideoFrame] {
  /** Our actual behavior. */
  override def receive: Receive = {
    case ActorPublisherMessage.Request(elements) =>
      while(totalDemand > 0) onNext(snap())
    case ActorPublisherMessage.Cancel => cam.close()
      context stop self
  }

  // Grab a webcam snapshot.
  def snap(): VideoFrame = {
    if(!cam.isOpen) cam.open()
    VideoFrame(cam.getImage, System.nanoTime, TimeUnit.NANOSECONDS)
  }
}
