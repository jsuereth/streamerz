package com.jsuereth.image

import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Utilities for resizing images
 */
object Resizer {

  // Note: This is borrowed from https://github.com/cb372/scala-ascii-art/blob/master/src/main/scala/com/github/cb372/asciiart/Asciifier.scala
  /** Forces scaling an image to the new size. */
  def forcedScale(image: BufferedImage, width: Int, height: Int): BufferedImage = {
    val scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val gfx = scaledImage.createGraphics()
    gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    gfx.drawImage(image, 0, 0, width, height, null)
    gfx.dispose
    scaledImage
  }


  /* A version of scaling that attempts to make the image as big as possible while preserving the aspect ratio. */
  def preserveRatioScale(image: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage = {
    // Quick algorithm to pick the biggest possible width/height while preserving size.
    // Note:  We could extract this into its own method.
    val ratio = image.getWidth().toDouble / image.getHeight()
    val testWidth = (ratio * maxHeight + 0.5).toInt
    val (width, height) =
    if(testWidth > maxWidth) {
      val iratio = image.getHeight().toDouble / image.getWidth()
      val testHeight = (ratio * maxWidth + 0.5).toInt
      (maxWidth, testHeight)
    } else {
      (testWidth, maxHeight)
    }

    forcedScale(image, width, height)
  }
}
