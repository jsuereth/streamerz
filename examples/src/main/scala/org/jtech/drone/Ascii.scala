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
 *     + Save half the space on colors by using CSS shorthand 12bit format
 *     + Client knows resolution, will split string in lines and color the chars
 *     + JSON -> base64 -> zip
 * - Serializer has only one `for yield`, functional, no more vars
 */
object Ascii extends App {

  type Pixel = Array[Byte]
  case class AsciiPicture(colors: Vector[String], chars: String)

  val palette = Vector(" ", ".", ",", ":", "*", "=", "+", "$", "%", "@", "A", "A", "#", "#")

  def chooseAsciiChar(pixel: Pixel, palette: Vector[String] = palette): String = {
    // Grey value (light intensity)
    val value = pixel.map(_ & 0xFF).sum / 3.0  // Bitwise and with 0xFF to remove byte sign
    val index = ((value / 255.0) * palette.length).toInt
    palette(index)
  }

  def shorthandColor(pixel: Pixel): String = {
    val formatted = pixel.map(b => (b & 0xFF) >> 4)  // Mask sign and keep most significant 4bits
    "%01x%01x%01x".format(formatted: _*)             // Format hex string, list unpacking trick
  }

  private def asciify(image: BufferedImage): AsciiPicture = {

    val pixels = image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData

    val picture = (for (pixel <- pixels.grouped(3).toVector) yield {
      (shorthandColor(pixel), chooseAsciiChar(pixel))
    }).unzip

    AsciiPicture(picture._1, picture._2.mkString)
  }

  def toJSON(image: BufferedImage): String = {
    write(asciify(image))
  }

  def test = {
    val testImage = new BufferedImage(20, 20, BufferedImage.TYPE_3BYTE_BGR)
    // Fill with gradient
    for (x <- 0 until testImage.getWidth; y <- 0 until testImage.getHeight) {
      val value = ((x * 1.0 / testImage.getWidth) * 255).toInt
      testImage.setRGB(x, y, value << 16 | value << 8 | value)
    }
    println(toJSON(testImage))
  }

  test
}
