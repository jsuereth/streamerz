package examples

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.jsuereth.video.AsciiVideoFrame

/**
 * Examplewhich downloads a video and renders it in ascii.
 */
object AsciiVideo {
  def main(args: Array[String]): Unit = {
    //val url = "http://vdownload.eu/webproxy/12409v4/55134921/3rdparty/e2c873d2d1.480.mp4/Rick%20Roll%20%E2%80%94%20Never%20Gonna%20Give%20You%20Up.mp4"
    val url = "file:///Users/pimverkerk/Downloads/small.mp4" //new java.io.File("rick.mp4").toURI.toASCIIString
    //val url = new java.io.File("goose.mp4").toURI.toASCIIString
    implicit val system = ActorSystem()
    val settings = ActorMaterializerSettings.create(system)
    val video = Source(com.jsuereth.video.ffmpeg.readVideoURI(new java.net.URI(url), system, playAudio = false))
    val asciifier = com.jsuereth.video.AsciiVideo.pixelAscii
    val terminal: Sink[AsciiVideoFrame, Unit] = Sink(com.jsuereth.video.Terminal.terminalMoviePlayer(system))
    asciifier.to(terminal).runWith(video)(ActorMaterializer(settings))
  }
}
