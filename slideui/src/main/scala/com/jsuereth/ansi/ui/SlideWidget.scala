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
          |                                                                             """.stripMargin,
      s"""
          |${Ansi.BOLD}Test SLIDE${Ansi.RESET_COLOR}
          |* Some ${Ansi.RED}Stuff${Ansi.RESET_COLOR}
          |* Some more stuff                                    """.stripMargin,
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

  private val renderedSlide = currentSlide map { case slide =>
    val lines = slide.split("[\r\n]+")
    val maxHeight = size().height
    val maxWidth = size().width
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
