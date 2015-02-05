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
     toAscii(image, Ansi.BACKGROUND_COLOR, _ => ' ')

  /** Convert an image to foreground colored ascii characters. */
  def toCharacterColoredAscii(image: BufferedImage): String =
    toAscii(image, Ansi.FOREGROUND_COLOR, chooseAsciiChar)

  /** Converts an image to raw ascii characters (no color) by intensity. */
  def toRawAscii(image: BufferedImage): String =
    toAscii(image, _ => "", chooseAsciiChar)


  // Note:  Borrowed from // Note: This is borrowed from https://github.com/cb372/scala-ascii-art/blob/master/src/main/scala/com/github/cb372/asciiart/Asciifier.scala
  // Convert color magnitude into a character
  private val asciiChars = List('#','A','@','%','$','+','=','*',':',',','.',' ')
  /** Converts a color to an ascii character based on its intensity. */
  def chooseAsciiChar(color: Color) = {
    def rgbMax =
      math.max(color.getRed, math.max(color.getGreen, color.getBlue))
    rgbMax match {
      case 0 => asciiChars.last
      case n => {
        val index = ((asciiChars.length * (rgbMax.toFloat / 255)) - (0.5)).toInt
        asciiChars(index)
      }
    }
  }

  // NOTE - this is ugly because we try to compress the string while we render.
  //        Avoding writing the control codes has hugely noticable affect on rendering.
  private def toAscii(image: BufferedImage, colorator: Color => String, pixelator: Color => Char): String = {
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
}
