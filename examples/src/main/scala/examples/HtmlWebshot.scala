package examples

import java.io.{FileWriter, FileOutputStream}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.Source
import com.jsuereth.image.{Resizer, Ascii}

import scala.concurrent.Future
import scala.util.Success

/**
 * Created by jsuereth on 2/25/15.
 */
object HtmlWebshot {
  implicit val system = ActorSystem("webcam-ascii-snap")

  def next(): Future[String] = {
    val result = concurrent.Promise.apply[String]
    val webcam = Source(com.jsuereth.video.WebCam.default(system))
    val settings = ActorMaterializerSettings.create(system)
    implicit val materializer = ActorMaterializer(settings)
    webcam.take(1).runForeach { frame =>
      val image = Ascii.toCharacterColoredHtml(Resizer.preserveRatioScale(frame.image, 80, 50))
      result.success(
        s"""
           |<html>
           |  <head><title>ascii snap</title></head>
           |  <body>
           |    $image
           |  </body>
           |</html>
         """.stripMargin)
    }
    result.future
  }


  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val out = new FileWriter("webcamsnap.html")
    next() onComplete {
      case Success(image) =>
        System.err.println("WRiting image!")
        out.write(image)
        out.close()
        //system.shutdown()
      case _ =>
        System.err.println("Failed to grab camera!")
        out.close()
        system.shutdown()
    }
  }
}
