package com.jsuereth.video
package swing

import javax.swing.{JComponent, JPanel, JFrame}
import akka.actor.{ActorRef, ActorSystem}
import java.awt.{BorderLayout, Component, GridLayout, Dimension}
import java.awt.event.{WindowAdapter, WindowEvent}


/** Widget which wraps another component, and exposes play/pause/stop buttons. */
class VideoPlayerDisplay(display: JComponent, controls: JComponent, width: Int, height: Int) extends JPanel {
  setLayout(new BorderLayout)
  add(display, BorderLayout.CENTER)
  //add(new JLabel("Video Player"), BorderLayout.CENTER)
  add(controls, BorderLayout.SOUTH)
  setMinimumSize(new Dimension(width, height))
}
