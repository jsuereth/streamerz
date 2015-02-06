package com.jsuereth.video
package swing

import akka.actor.{ActorRefFactory, Props}
import javax.swing.{JPanel,JButton}
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JComponent
import akka.actor.ActorRef
import akka.stream.actor.ActorPublisher
import org.reactivestreams.Publisher

private[swing] case object PlayPauseButtonClicked
private[swing] case object StopButtonClicked

/** Play pause controls for the UI. */
private[swing] class PlayerControls(actor: ActorRef) extends JPanel {
  private val playPauseButton = new JButton("play/pause")
  private val stopButton = new JButton("stop")
  setPreferredSize(new Dimension(640, 100))
  setMinimumSize(new Dimension(640, 100))
  setLayout(new GridLayout(1, 2))
  playPauseButton.setMinimumSize(new Dimension(200, 100))
  stopButton.setMinimumSize(new Dimension(200, 100))
  add(playPauseButton)
  add(stopButton)
  playPauseButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent): Unit =
      actor ! PlayPauseButtonClicked
  })
  stopButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent): Unit =
      actor ! StopButtonClicked
  })

}
private[swing] class PlayerControlsActor extends ActorPublisher[UIControl] {
  sealed trait State
  case object Playing extends State
  case object Paused extends State
  case object Stopped extends State
  private var state: State = Stopped
  override def receive: Receive = {
    case PlayPauseButtonClicked =>
      state match {
        case Playing =>
          state = Paused
          onNext(Pause)
        case _ =>
          state = Playing
          onNext(Play)
      }
    case StopButtonClicked =>
      state match {
        case Stopped =>  // ignore, already in correct state.
        case _ =>
          state = Stopped
          onNext(Stop)
      }
  }
}
object PlayerControls  {
  /** Construct a panel with player controls which produce UIControl events. */
  def apply(factory: ActorRefFactory): (Publisher[UIControl], JComponent) = {
    val actorRef = factory.actorOf(Props[PlayerControlsActor].withDispatcher("swing-dispatcher"), "video-controls")
    val component = new PlayerControls(actorRef)
    ActorPublisher(actorRef) -> component
  }
}