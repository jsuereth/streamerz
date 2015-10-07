package com.jsuereth.video

import java.awt.image._
import java.util.concurrent._


/** Fundamental unit of video. */
case class VideoFrame(image: BufferedImage, timeStamp: Long, timeUnit: TimeUnit)
