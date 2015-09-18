package com.jsuereth.video

import java.awt.image.BufferedImage
import akka.stream.scaladsl._
import com.jsuereth.image.{Resizer, Ascii}

/**
 * Helpers for Ascii video streaming.
 *
 * TODO - move this into subpackage.
 */
object AsciiVideo {
  // TODO - allow width/height to be set.

  /** Converts a video into large-pixel ascii.  This will use spaces and background colors. */
  val pixelAscii = asciiConversion(Ascii.toBackgroundAsciiSpaces)
  /** Converts a video into black and white ascii charaters. */
  val bwAscii = asciiConversion(Ascii.toRawAscii)
  /** Converts a video into colored character ascii. */
  val colorAscii = asciiConversion(Ascii.toCharacterColoredAscii)


  def asciiConversion(asciifier: BufferedImage => String, maxWidth: Int = 80, maxHeight: Int = 40): Flow[VideoFrame, AsciiVideoFrame, Unit] = {
    Flow[VideoFrame].map { frame =>
      AsciiVideoFrame(asciifier(Resizer.preserveRatioScale(frame.image, maxWidth, maxHeight)), frame.timeStamp, frame.timeUnit)
    }
  }
}
