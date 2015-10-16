package org.jtech.drone

import java.awt.image.{DataBufferByte, BufferedImage}

import upickle.default.write

/**
 * - Operations get repeated a lot, need to pre-cook things:
 *     + Array is directly Strings instead of casting to Char at every loop iteration
 *     + BufferedImage.getRGB() is very slow: get whole pixel data first
 *       benchmark: http://stackoverflow.com/a/9470843
 * - chooseAsciiChar was too complex and wrong
 * - HTML is way too big:
 *     + Switching to JSON, with an array of colors and a long String of chars
 *     + Client knows resolution, will split string in lines and color the chars
 *     + JSON -> base64 -> zip
 * - Serializer has only one `for yield`, functional, no more vars
 */
object Ascii {

  type Pixel = Array[Byte]
  case class AsciiPicture(colors: Seq[String], chars: String)

  val palette = Vector(" ", ".", ",", ":", "*", "=", "+", "$", "%", "@", "A", "A", "#", "#")

  def chooseAsciiChar(pixel: Pixel, palette: Array[String] = palette): String = {
    // Grey value (light intensity)
    val value = pixel.sum / 3.0
    val index = ((value / 255.0) * palette.length).toInt
    palette(index)
  }

  private def asciify(image: BufferedImage): AsciiPicture = {

    val pixels = image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData

    val picture = (for (pixel <- pixels.grouped(3).toVector) yield {
      val char = chooseAsciiChar(pixel)
      val color = "%02x%02x%02x".format(pixel(0), pixel(1), pixel(2))
      (color, char)
    }).unzip

    AsciiPicture(picture._1, picture._2.mkString)
  }

  def toJSON(image: BufferedImage): String = {
    write(asciify(image))
  }

}
