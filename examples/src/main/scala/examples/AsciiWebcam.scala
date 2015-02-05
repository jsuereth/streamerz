package examples

import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.video.AsciiVideoFrame

/**
 * Renders your webcam in ascii to the terminal.
 */
object AsciiWebcam {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    val settings = MaterializerSettings.create(system)
    val video = Source(com.jsuereth.video.WebCam.default(system))
    val asciifier = com.jsuereth.video.AsciiVideo.pixelAscii
    val terminal: Sink[AsciiVideoFrame] = Sink(com.jsuereth.video.Terminal.terminalMoviePlayer(system))
    asciifier.to(terminal).runWith(video)(FlowMaterializer(settings))
  }
}
