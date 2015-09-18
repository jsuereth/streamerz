package com.jsuereth.image

import java.awt.Color
import java.awt.image.BufferedImage

import com.jsuereth.ansi.Ansi

/**
 * Utilities for converting images to Ascii art.
 */
object Ascii {

  /** Convert an image to large pixels.  This uses whitespace and background color. */
  def toBackgroundAsciiSpaces(image: BufferedImage): String =
     toAscii(image, Ansi.BACKGROUND_COLOR, _ => "  ")

  /** Convert an image to foreground colored ascii characters. */
  def toCharacterColoredAscii(image: BufferedImage): String =
    toAscii(image, Ansi.FOREGROUND_COLOR, x => chooseAsciiChar(x))

  /** Convert an image to foreground colored ascii characters. */
  def toCharacterColoredAscii2x(image: BufferedImage): String =
    toAscii(image, Ansi.FOREGROUND_COLOR, { x =>
      val c = chooseAsciiChar(x)
      s"$c$c"
    })

  /** Converts an image to raw ascii characters (no color) by intensity. */
  def toRawAscii(image: BufferedImage): String =
    toAscii(image, _ => "", x => chooseAsciiChar(x))


  def toCharacterColoredHtml(image: BufferedImage): String =
    toHtml(image, x => chooseAsciiChar(x))




  // Note:  Borrowed from // Note: This is borrowed from https://github.com/cb372/scala-ascii-art/blob/master/src/main/scala/com/github/cb372/asciiart/Asciifier.scala
  // Convert color magnitude into a character.   This has been adapted for dark-background terminals, which a lot of us use
  // by default (just by reversing the list).
  val darkBackgroundIntensityPallet = Array('#','#','A','A','@','%','$','+','=','*',':',',','.',' ').reverse



  /** Converts a color to an ascii character based on its intensity. */
  def chooseAsciiChar(color: Color, intensityPallete: Array[Char] = darkBackgroundIntensityPallet): String = {
    def rgbMax =
      math.max(color.getRed, math.max(color.getGreen, color.getBlue))
    rgbMax match {
      case 0 => intensityPallete.last.toString
      case n => {
        val index = ((intensityPallete.length * (rgbMax.toFloat / 255)) - (0.5)).toInt
        intensityPallete(index).toString
      }
    }
  }

  // NOTE - this is ugly because we try to compress the string while we render.
  //        Avoding writing the control codes has hugely noticable affect on rendering.
  private def toAscii(image: BufferedImage, colorator: Color => String, pixelator: Color => String): String = {
    case class MyState(lastColor: String, buf: StringBuilder)
    val buf = new StringBuilder("")
    var lastColor = ""
    var y = 0
    while(y < image.getHeight) {
      var x = 0
      while(x < image.getWidth) {
        val pixel = new Color(image.getRGB(x,y))
        val color = colorator(pixel)
        if(lastColor != color) buf.append(color)
        val char = pixelator(pixel)
        buf.append(char)
        lastColor = color
        x += 1
      }
      buf.append("\n")
      lastColor=""
      y += 1
    }
    buf.toString()
  }
  private def toHtmlColorHex(color: Color): String = {
    val hex = Integer.toHexString(color.getRGB)
    hex.substring(2, hex.length)
  }
  private def toHtmlString(char: String): String =
    char.replaceAllLiterally(" ", "&nbsp;")
  // NOTE - this should be unified with the above...
  private def toHtml(image: BufferedImage, pixelator: Color => String): String = {
    case class MyState(lastColor: String, buf: StringBuilder)
    val buf = new StringBuilder("""<p style="
                                  |     font-family:Courier,monospace;
                                  |     font-size:5pt;
                                  |     letter-spacing:1px;
                                  |     line-height:4pt;
                                  |     font-weight:bold;
                                  |     background-color: black;"><span style="display:inline">""".stripMargin)
    var lastColor = ""
    var y = 0
    while(y < image.getHeight) {
      var x = 0
      while(x < image.getWidth) {
        val pixel = new Color(image.getRGB(x,y))
        val color = toHtmlColorHex(pixel)
        if(lastColor != color) {
          // TODO - only end a span if one was created.
          buf.append("</span>")
          buf.append(s"""<span style="display:inline; color: #${color}">""")
        }
        val char = pixelator(pixel)
        buf.append(toHtmlString(char))
        lastColor = color
        x += 1
      }
      buf.append("<br/>")
      lastColor=""
      y += 1
    }
    buf.append("</p>").toString()
  }
}
