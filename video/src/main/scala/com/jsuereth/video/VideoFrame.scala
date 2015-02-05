package com.jsuereth.video

import java.awt.image.BufferedImage
import java.io.{ObjectInputStream, ObjectOutputStream}
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/** Fundamental unit of video. */
case class VideoFrame(image: BufferedImage, timeStamp: Long, timeUnit: TimeUnit)
