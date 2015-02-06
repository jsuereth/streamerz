package com.jsuereth.video

import java.io.File
import java.net.URI

import akka.actor.ActorRefFactory
import org.reactivestreams.Publisher

/**
 * Helpers for reading ffmpeg files.
 */
package object ffmpeg {
  /** Reads a given file and pushes its stream events out.
    * Note: This will not prefetch any data, but only read when requested.
    */
  def readVideoFile(file: File, system: ActorRefFactory, playAudio: Boolean = false): Publisher[VideoFrame] =
    FFMpegPublisher(system, file.toURI, playAudio)

  /** Reads from a URI. */
  def readVideoURI(file: URI, system: ActorRefFactory, playAudio: Boolean = false): Publisher[VideoFrame] =
    FFMpegPublisher(system, file, playAudio)

}
