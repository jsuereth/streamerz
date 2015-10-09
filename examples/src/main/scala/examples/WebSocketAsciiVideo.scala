package examples

import akka.actor.ActorSystem
import akka.stream.ActorMaterializerSettings
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.ActorMaterializer
import com.jsuereth.video.AsciiVideoFrame
import akka.util.Timeout
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import scala.concurrent.duration.DurationInt
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import com.jsuereth.video.AsciiVideoFrame
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import com.jsuereth.image.Ascii
import com.jsuereth.video.AsciiVideo.asciiConversion

object WebSocketAsciiVideo extends App{
  implicit val system = ActorSystem()
  
  val settings = ActorMaterializerSettings.create(system)
  val video = Source(com.jsuereth.video.WebCam.default(system))
  val asciifier = asciiConversion(Ascii.toCharacterColoredHtml, maxWidth = 80, maxHeight = 40)
  
  implicit val executor = system.dispatcher
  implicit val timeout = Timeout(1000.millis)

  implicit val materializer = ActorMaterializer()
  val serverBinding = Http().bindAndHandle(interface = "0.0.0.0", port = 8080, handler = mainFlow)
  
  def mainFlow = path("") {
    val socketFlow: Flow[Message, Message, Unit] = 
      Flow.wrap(Sink.ignore, video.via(asciifier).map(toMessage))(Keep.none)
    handleWebsocketMessages(socketFlow)
  }
  
  def toMessage(frame: AsciiVideoFrame): Message = {
    //println(frame.image)
    TextMessage.Strict(frame.image)
  }
}