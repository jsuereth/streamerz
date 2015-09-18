package com.jsuereth.video
package ffmpeg

import java.io.File
import java.net.URI
import akka.stream.actor.{ActorPublisherMessage, ActorPublisher}
import com.xuggle.mediatool.{IMediaListener, IMediaViewer, ToolFactory, MediaListenerAdapter}
import com.xuggle.mediatool.event._
import com.xuggle.xuggler.Utils
import com.xuggle.xuggler.IError
import akka.actor.{Props, ActorRefFactory}
import org.reactivestreams.Publisher


private[ffmpeg] case class FFMpegError(raw: IError) extends Exception(raw.getDescription)

/** An actor which reads the given file on demand. */
private[ffmpeg] class FFMpegPublisher(file: URI, playAudio: Boolean) extends ActorPublisher[VideoFrame] {
  private var closed: Boolean = false
  private var frameCount: Long = 0L

  /** Open the reader. */
  private val reader = ToolFactory.makeReader(file.toASCIIString)//file.getAbsolutePath)
  /** Register a listener that will forward all events down the Reactive Streams chain. */
  reader.addListener(new MediaListenerAdapter() {
    override def onVideoPicture(e: IVideoPictureEvent): Unit = {
      if(e.getMediaData.isComplete) {
        onNext(VideoFrame(Utils.videoPictureToImage(e.getMediaData), e.getTimeStamp, e.getTimeUnit))
        frameCount += 1
      }
    }
  })
  if(playAudio) {
    // Note: Xuggle requires the classloader be the system classloader, if it uses Java audio which is broken.
    //val old = Thread.currentThread().getContextClassLoader
    //Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader)
    //try {
    val audioPlayback = new AudioPlayer()
    reader.addListener(audioPlayback)
    //} finally Thread.currentThread().setContextClassLoader(old)
  }
  /** Our actual behavior. */
  override def receive: Receive = {
    case ActorPublisherMessage.Request(elements) => read(elements)
    case ActorPublisherMessage.Cancel =>
      reader.close()
      context stop self
  }

  // Reads the given number of frames, or bails on error.
  // Note: we have to track frames via the listener we have on the reader.
  private def read(frames: Long): Unit = {
    val done = frameCount + frames
    // Close event should automatically occur.
    while(!closed && frameCount < done) {
      try (reader.readPacket match {
        case null => // Ignore
        case error =>
          // Ensure we're closed.
          closed = true
          if(error.getType == IError.Type.ERROR_EOF) onComplete()
          else onError(FFMpegError(error))
      }) catch {
        // Some sort of fatal read error.
        case e: Exception =>
          closed = true
          onError(e)
      }
    }
  }
}
private[ffmpeg] object FFMpegPublisher {
  def apply(factory: ActorRefFactory, file: URI, playAudio: Boolean = false): Publisher[VideoFrame] =
    ActorPublisher(factory.actorOf(Props(new FFMpegPublisher(file, playAudio))))
}
// A helper which ensures classloader is correct before any attempts hit Java Sound.
private class AudioPlayer() extends IMediaListener {
  private val viewer = withSystemClassLoader(ToolFactory.makeViewer(IMediaViewer.Mode.AUDIO_ONLY))

  override def onVideoPicture(iVideoPictureEvent: IVideoPictureEvent): Unit =
    viewer.onVideoPicture(iVideoPictureEvent)
  override def onWriteHeader(iWriteHeaderEvent: IWriteHeaderEvent): Unit =
    viewer.onWriteHeader(iWriteHeaderEvent)
  override def onFlush(iFlushEvent: IFlushEvent): Unit =
    viewer.onFlush(iFlushEvent)
  override def onOpenCoder(iOpenCoderEvent: IOpenCoderEvent): Unit =
    withSystemClassLoader(viewer.onOpenCoder(iOpenCoderEvent))
  override def onAudioSamples(iAudioSamplesEvent: IAudioSamplesEvent): Unit =
    viewer.onAudioSamples(iAudioSamplesEvent)
  override def onWritePacket(iWritePacketEvent: IWritePacketEvent): Unit =
    viewer.onWritePacket(iWritePacketEvent)
  override def onCloseCoder(iCloseCoderEvent: ICloseCoderEvent): Unit =
    viewer.onCloseCoder(iCloseCoderEvent)
  override def onClose(iCloseEvent: ICloseEvent): Unit =
    viewer.onClose(iCloseEvent)
  override def onWriteTrailer(iWriteTrailerEvent: IWriteTrailerEvent): Unit =
    viewer.onWriteTrailer(iWriteTrailerEvent)
  override def onOpen(iOpenEvent: IOpenEvent): Unit =
    withSystemClassLoader(viewer.onOpen(iOpenEvent))
  override def onReadPacket(iReadPacketEvent: IReadPacketEvent): Unit =
    viewer.onReadPacket(iReadPacketEvent)
  override def onAddStream(iAddStreamEvent: IAddStreamEvent): Unit =
    withSystemClassLoader(viewer.onAddStream(iAddStreamEvent))


  private def withSystemClassLoader[A](f: => A) = {
    val thread = Thread.currentThread()
    val old = thread.getContextClassLoader
    val systemCl = ClassLoader.getSystemClassLoader
    thread.setContextClassLoader(systemCl)
    try f
    catch {case e: Exception => 
      println("Exception: " + e)
      throw e
    }
    finally thread.setContextClassLoader(old)
  }
}