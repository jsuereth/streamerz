package com.jsuereth.ansi.ui

import com.jsuereth.ansi.Ansi
import org.fusesource.jansi.AnsiString

import scala.reactive.{Signal, Reactive}

sealed trait SlideControlEvent
case class NextSlide() extends SlideControlEvent
case class PreviousSlide() extends SlideControlEvent
case class FirstSlide() extends SlideControlEvent
case class LastSlide() extends SlideControlEvent

/**
 * Created by jsuereth on 3/4/15.
 */
object SlideWidget {
  // TODO - Implement
  def loadSlides() =
    Seq(
      s"""
          |  ${Ansi.BOLD} A Whirlwind Tour${Ansi.RESET_COLOR}
          |  ${Ansi.ITALIC}of the ${Ansi.RED}Scala${Ansi.RESET_COLOR}${Ansi.ITALIC} Ecosystem${Ansi.RESET_COLOR}
          |""".stripMargin,
      s"""
          |${Ansi.BOLD}Agenda${Ansi.RESET_COLOR}
          |* The core ${Ansi.RED}Scala${Ansi.RESET_COLOR} libraries
          |* The ${Ansi.CYAN}early wave${Ansi.RESET_COLOR} of libraries
          |* Up and coming
          |* Tooling""".stripMargin,
    s"""| --===  ${Ansi.BOLD}Core Scala${Ansi.RESET_COLOR} ==--
        |
        |* Standard Library
        |  - Collections
        |  - Futures
        |  - Option, Try
        |  - Process
        |* Modules
        |  - Actors
        |  - Parser Combinators
        |  - XML
     """.stripMargin,
      s"""| --===  ${Ansi.BOLD}The early wave${Ansi.RESET_COLOR} ==--
          |
          |* Akka, Spray
          |* Lift
          |* Unfiltered
          |* Dispatch
          |* Scalaz
          |* Play
          |* Spire
          |* Scalatest, Specs, Scalacheck
          |* sbt
     """.stripMargin,
      s"""| --===  ${Ansi.BOLD}Up and Coming${Ansi.RESET_COLOR} ==--
        |
        |* Spark
        |* Akka Streams, Akka Http
        |* Reactive Collections
        |* cats, algebra, etc.
        |* Scalaz-Streams
        | - TODO - more
        """.stripMargin,
      s"""
          |
          |              FIN
          |
          |                                             """.stripMargin
    )
}

class SlideWidget(renders: Reactive.Emitter[DisplayText], control: Reactive[SlideControlEvent], size: Signal[ConsoleSize]) {
  val slides = SlideWidget.loadSlides()

  val (setSlide, currentSlideIdx) = {
    val e = new Reactive.Emitter[Int]
    (e, e.signal(0))
  }

  private val sub = control foreach {
    case NextSlide() =>
      val current = currentSlideIdx()
      if(current + 1 < slides.length) setSlide += (current + 1)
    case PreviousSlide() =>
      val current = currentSlideIdx()
      if(current - 1 >= 0) setSlide += (current - 1)
    case FirstSlide() => setSlide += 0
    case LastSlide() => setSlide += slides.length - 1
  }

  // TODO - we need to resize the slide so that it covers the entire area we own....
  private val currentSlide = currentSlideIdx map { idx => slides(idx) }


  private val renderedSlide = (currentSlide zip size) { case (slide, s) =>
    val lines = slide.split("[\r\n]+")
    val maxHeight = s.height
    val maxWidth = s.width
    import Padding._
    val padded: Seq[String] = for(line <- lines) yield {
      val realSize = new AnsiString(line).length
      if(realSize < maxWidth) s"$line${pad(maxWidth - realSize)}"
      else line
    }
    (padded ++ padLines(maxHeight - lines.length, maxWidth)).mkString("\n")
  }

  private val renderSub = renderedSlide foreach { s =>
    renders += DisplayText(s"${Ansi.MOVE_CURSOR_TO_UPPER_LEFT}$s")
  }

}
