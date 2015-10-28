package org.jtech.drone

import java.awt.image.{DataBufferInt, BufferedImage}
import java.io.File
import java.util.Base64
import java.util.zip.Deflater
import javax.imageio.ImageIO

import com.typesafe.scalalogging.LazyLogging

/**
 * - Operations get repeated a lot, need to pre-cook things:
 *     + Array is directly Strings instead of casting from char at every loop iteration
 *     + BufferedImage.getRGB() is slow: get whole pixel data in a buffer     first
 *       benchmark: http://stackoverflow.com/a/9470843
 * - chooseAsciiChar was too complex and wrong
 * - HTML is way too big:
 *     + Switching to JSON, with an array of colors and a long String of chars
 *     + Client knows resolution, will split string in lines and color the chars
 *     + JSON -> base64 -> zip
 * - Made things more functional
 */
object Ascii extends LazyLogging {

  type AsciiPicture = ((Int, Int), Vector[String], String)

  def chooseAsciiChar(color: Int, palette: Vector[String]): String = {
    // Average value of RGB components
    val value = ((color & 0xff) + ((color & 0xff00) >> 8) + ((color & 0xff0000) >> 16)) / 3.0
    val index = ((value / 255.0) * (palette.length - 1)).toInt
    palette(index)
  }

  def toHexString(color: Int): String = {
    // Manual string format is way too slow: +40ms
    Integer.toHexString(color).substring(2, 8) // Trick only works with TYPE_INT_ARGB
  }

  def correctFormat(image: BufferedImage): BufferedImage = {
    val corrected = new BufferedImage(image.getWidth, image.getHeight, BufferedImage.TYPE_INT_ARGB)
    corrected.getGraphics.drawImage(image, 0, 0, null)
    corrected
  }

  def asciify(image: BufferedImage): AsciiPicture = {

    val pixels = image.getRaster.getDataBuffer.asInstanceOf[DataBufferInt].getData
    val palette = Vector(" ", ".", ",", ":", "*", "=", "+", "$", "%", "@", "A", "A", "#", "#")

    val picture = pixels.toVector.map { color =>
      (toHexString(color), chooseAsciiChar(color, palette))
    }.unzip

    val size = (image.getWidth, image.getHeight)

    (size, picture._1, picture._2.mkString)
  }

  def toJSON(asciiPicture: AsciiPicture): Array[Byte] = {
    val buffer = new StringBuilder()
    buffer.append("{\"colors\":[")
    buffer.append(asciiPicture._2.map(s => "\""+s(0)+s(2)+s(4)+"\"").mkString(","))
    buffer.append("],\"chars\":\"")
    buffer.append(asciiPicture._3)
    buffer.append("\",")
    buffer.append("\"width\":" + asciiPicture._1._1 + ",")
    buffer.append("\"height\":" + asciiPicture._1._2 + "}")
    buffer.toString.getBytes
  }

  def toBase64(input: Array[Byte]): String = {
    new String(Base64.getEncoder.encode(input))
  }

  def compress(input: Array[Byte]): Array[Byte] = {
    val deflater = new Deflater(Deflater.BEST_COMPRESSION)
    deflater.setInput(input)
    deflater.finish()

    val compressed = new Array[Byte](input.length * 2)
    val size = deflater.deflate(compressed)

    compressed.take(size)
  }

  // Nice function from http://stackoverflow.com/a/9160068
  def time[R](message: String = "")(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val ms = (t1 - t0) / 1000000
    val ps = if (ms > 0) "%5d/s".format(1000 / ms) else "    INF"
    logger.info("%-12s%3dms  %s".format(message, ms, ps))
    result
  }

  def test() = {

    val testImage = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB)
    // Fill with gradient
    for (x <- 0 until testImage.getWidth; y <- 0 until testImage.getHeight) {
      val value = ((x * 1.0 / testImage.getWidth) * 255).toInt
      testImage.setRGB(x, y, 0xff << 24 | value << 16 | value << 8 | value)
    }

    val root = new File("").getAbsolutePath
    val testImage2 = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB)
    val einstein = ImageIO.read(new File(root + "/src/main/scala/org/jtech/drone/einstein.png"))
    val g = testImage2.createGraphics
    g.drawImage(einstein, 0, 0, null)
    g.dispose()

    logger.info("=" * 26)
    logger.info(" " * 10 + "TIMES")
    logger.info("=" * 26)

    val oldMethod = time("HTML") {
      com.jsuereth.image.Ascii.toCharacterColoredHtml(testImage2)
    }
    val oldZipped = time("ZIP") {
      compress(oldMethod.getBytes)
    }
    val oldBase64 = time("Base64") {
      toBase64(oldZipped)
    }

    logger.info("-" * 26)

    val ascii = time("ASCIIfy") {
      asciify(testImage2)
    }
    val json = time("JSON") {
      toJSON(ascii)
    }
    val zipped = time("ZIP") {
      compress(json)
    }
    val base64 = time("Base64") {
      toBase64(zipped)
    }

    logger.info("=" * 26)
    logger.info(" " * 10 + "SIZES")
    logger.info("=" * 26)

    def printResult(title: String, length: Int) {
      logger.info("%-10s%6dB %6.1fKB".format(title, length, length / 1000.0))
    }

    printResult("HTML", oldMethod.length)
    printResult("ZIP", oldZipped.length)
    printResult("Base64", oldBase64.length)

    logger.info("-" * 26)

    printResult("JSON", json.length)
    printResult("ZIP", zipped.length)
    printResult("Base64", base64.length)
  }

}
