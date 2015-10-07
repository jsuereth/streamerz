package com.jsuereth.video

import java.awt.image.BufferedImage
import java.io._
import java.util.concurrent.TimeUnit


/** Fundamental unit of video. */
case class VideoFrame(image: BufferedImage, timeStamp: Long, timeUnit: TimeUnit)
