package com.jsuereth.ansi.ui

import java.awt.Color

import com.jsuereth.ansi.{AnsiStringUtils, Ansi}
import com.jsuereth.ansi.markdown.MarkdownToAnsi
import com.jsuereth.ansi.ui.frp.layout.{Padding, ConsoleLayout}

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
  private def url(u: String): String = s"${Ansi.UNDERLINE}${Ansi.BLUE}$u${Ansi.RESET_COLOR}"
  private val B = s"${Ansi.FOREGROUND_COLOR(Color.GRAY)}*${Ansi.RESET_COLOR}"
  private val D = s"${Ansi.FOREGROUND_COLOR(Color.GRAY)}-${Ansi.RESET_COLOR}"
  // TODO - Implement
  def loadSlides(directory: java.io.File = new java.io.File("slides/whirlwind-tour-scala-ecosystem")): Seq[String] = {
    val files = Option(directory.listFiles()).getOrElse(Array.empty[java.io.File]).filter(_.getName endsWith ".md").sortBy(_.getName)
    files map { f =>
      val s = scala.io.Source.fromFile(f)
      val contents =
        try s.getLines.mkString("\n")
        finally s.close()
      MarkdownToAnsi.convert(contents)
    }
  }
}

class SlideWidget(renders: Reactive.Emitter[DisplayText], control: Reactive[SlideControlEvent], layout: Signal[ConsoleLayout]) {
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


  private val renderedSlide = (currentSlide zip layout) { case (slide, l) =>
    val lines = slide.split("[\r]?[\n]")
    val maxHeight = l.size.height
    val maxWidth = l.size.width
    import Padding._
    val padded: Seq[String] = for(line <- lines) yield {
      val realSize = AnsiStringUtils.realLength(line)
      if(realSize < maxWidth) s"$line${pad(maxWidth - realSize)}"
      else line
    }
    val relocated =
      for((line, idx) <- (padded ++ padLines(maxHeight - lines.length, maxWidth)).zipWithIndex) yield {
        val row = l.pos.row + idx
        s"${Ansi.MOVE_CURSOR(row, l.pos.col)}$line"
      }
    relocated.mkString("")
  }

  private val renderSub = renderedSlide foreach { s =>
    renders += DisplayText(s"${Ansi.MOVE_CURSOR_TO_UPPER_LEFT}$s")
  }

}
