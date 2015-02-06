package com.jsuereth.video

import java.awt.event.{WindowEvent, WindowAdapter}
import java.awt.{GridLayout, Component}
import javax.swing.{JFrame, JComponent}

import akka.actor.{ActorRef, ActorSystem}
import org.reactivestreams.{Publisher, Subscriber}

/**
 * Helpers for creating swing components.
 */
package object swing {

  /** Creates a new JFrame where we can dump a VideoFrame stream and it will render in realtime. */
  def createPreviewSink(system: ActorSystem): Subscriber[VideoFrame] = {
    val (consumer, display) = swing.VideoPanel(system)
    createPreviewFrame(system, display)
    consumer
  }

  def createPreviewFrame(system: ActorSystem, display: JComponent, width: Int = 640, height: Int = 480) = {
    inFrame("Video Preview", display, system, width, height)
  }

  /** Creates a  new JFrame that has play/pause/stop controls.  We get an outcoming stream of control events and we need to provide
    * an ingoing stream of video feed.
    */
  def createRawVideoPlayer(system: ActorSystem, width: Int = 640, height: Int = 480): (Subscriber[VideoFrame], Publisher[UIControl]) = {
    val (consumer, display) = swing.VideoPanel(system)
    val (producer, controls) = swing.PlayerControls(system)
    val player = new VideoPlayerDisplay(display, controls, width, height)
    inFrame("Video Player", player, system, width, height)
    consumer -> producer
  }

  /** Creates a video player that can play/pause a single stream of video. */
  def createVideoPlayer(system: ActorSystem, openFile: () => Publisher[VideoFrame], width: Int = 640, height: Int = 480): Unit = {
    val (videoSink, uiSource) = createRawVideoPlayer(system, width, height)
    val (uiSink, videoSource) = PlayerProcessor.create(system, openFile)
    uiSource.subscribe(uiSink)
    videoSource.subscribe(videoSink)
  }


  private def inFrame[T](title: String, c: Component, system: ActorSystem, width: Int, height: Int): JFrame = {
    val jframe = new JFrame(title)
    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val pane = jframe.getContentPane
    pane.setLayout(new GridLayout(1, 1))
    pane.add(c)


    jframe.setSize(width, height)
    jframe.setVisible(true)
    jframe.addWindowListener(new WindowAdapter() {
      override def windowClosed(e: WindowEvent): Unit = {
        system.shutdown()
      }
    })
    jframe
  }
}
