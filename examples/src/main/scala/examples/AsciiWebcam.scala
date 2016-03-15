package examples

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.ansi.Ansi
import com.jsuereth.video.AsciiVideoFrame

/**
 * Renders your webcam in ascii to the terminal.
 */
object AsciiWebcam {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    val settings = ActorMaterializerSettings.create(system)
    val video = Source(com.jsuereth.video.WebCam.default(system))
    val asciifier =
      com.jsuereth.video.AsciiVideo.pixelAscii
      //com.jsuereth.video.AsciiVideo.colorAscii
    val terminal: Sink[AsciiVideoFrame, Unit] = Sink(com.jsuereth.video.Terminal.terminalMoviePlayer(system))
    asciifier.to(terminal).runWith(video)(ActorMaterializer(settings))
  }
}

// It seems heavyweight to always create an actor system for this...
object AsciiWebcamSnap {
  val asciifier = com.jsuereth.video.AsciiVideo.colorAscii
  def next() = {
    implicit val system = ActorSystem("webcam-ascii-snap")
    val webcam = Source(com.jsuereth.video.WebCam.default(system))
    val settings = ActorMaterializerSettings.create(system)
    val result = concurrent.Promise.apply[String]
    asciifier.take(1).runWith(webcam, Sink.foreach { frame =>
      result.success(frame.image + Ansi.RESET_COLOR)
    })(ActorMaterializer(settings))
    // ensure sbt console doesn't become unresponsive.
    import concurrent.ExecutionContext.Implicits.global
    result.future.onComplete {
      x => system.shutdown()
    }
    result.future
  }
}