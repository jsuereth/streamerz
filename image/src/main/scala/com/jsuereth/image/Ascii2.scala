package com.jsuereth.image

import java.awt.image.{BufferedImage, DataBufferInt}
import java.util.Base64
import java.util.zip.Deflater

object Ascii2 {

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

}
