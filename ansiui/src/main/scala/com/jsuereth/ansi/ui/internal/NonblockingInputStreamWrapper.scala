/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */

package com.jsuereth.ansi.ui.internal


import java.io.IOException
import java.io.InputStream

object NonblockingInputStreamWrapper {
  val READ_TIMEOUT = -2
}
/**
 * This class wraps a regular input stream and allows it to appear as if it
 * is non-blocking; that is, reads can be performed against it that timeout
 * if no data is seen for a period of time.  This effect is achieved by having
 * a separate thread perform all non-blocking read requests and then
 * waiting on the thread to complete.
 *
 * <p>VERY IMPORTANT NOTES
 * <ul>
 *   <li> This class is not thread safe. It expects at most one reader.
 *   <li> The {@link #shutdown()} method must be called in order to shut down
 *          the thread that handles blocking I/O.
 * </ul>
 *
 *
 *
 *  * Creates a <code>NonBlockingInputStream</code> out of a normal blocking
 * stream. Note that this call also spawn a separate thread to perform the
 * blocking I/O on behalf of the thread that is using this class. The
 * {@link #shutdown()} method must be called in order to shut this thread down.
 *
 * @author Scott C. Gray <scottgray1@gmail.com>
 * Adapted by J. Suereth for Scala/FRP-ANSI
 */
class NonBlockingInputStreamWrapper(in: InputStream) extends InputStream() {
  private var ch: Int = NonblockingInputStreamWrapper.READ_TIMEOUT            // Recently read character
  private var threadIsReading: Boolean = false
  private var isShutdown: Boolean = false
  private var exception: IOException  = null

  /* A thread we use to not block the input stream as we read. */
  private object myThread extends Thread("NonBlockingInputStreamThread") {
    override def run(): Unit = NonBlockingInputStreamWrapper.this.run()
  }
  myThread.setDaemon(true)
  myThread.start()

  /**
   * Shuts down the thread that is handling blocking I/O. Note that if the
   * thread is currently blocked waiting for I/O it will not actually
   * shut down until the I/O is received.  Shutting down the I/O thread
   * does not prevent this class from being used, but causes the
   * non-blocking methods to fail if called and causes {@link #isNonBlockingEnabled()}
   * to return false.
   */
  def shutdown(): Unit = synchronized {
    if (!isShutdown) {
      isShutdown = true
      notify()
    }
  }
  /*
   * The underlying input stream is closed first. This means that if the
   * I/O thread was blocked waiting on input, it will be woken for us.
   */
  override def close(): Unit = {
    in.close()
    shutdown()
  }

  override def read(): Int =
    read(0L, false)

  /**
   * Peeks to see if there is a byte waiting in the input stream without
   * actually consuming the byte.
   *
   * @param timeout The amount of time to wait, 0 == forever
   * @return -1 on eof, -2 if the timeout expired with no available input
   *   or the character that was read (without consuming it).
   * @throws IOException
   */
  def peek(timeout: Long): Int = {
    if (isShutdown) {
      throw new UnsupportedOperationException ("peek() cannot be called as non-blocking operation is disabled")
    }
    read(timeout, true)
  }

  /**
   * Attempts to read a character from the input stream for a specific
   * period of time.
   * @param timeout The amount of time to wait for the character
   * @return The character read, -1 if EOF is reached, or -2 if the
   *   read timed out.
   * @throws IOException
   */
  def read(timeout: Long): Int = {
    if (isShutdown) {
      throw new UnsupportedOperationException ("read() with timeout cannot be called as non-blocking operation is disabled")
    }
    read(timeout, false)
  }

  private def checkInputAndThrowIfError(isPeek: Boolean): Unit = {
    /*
    * If the thread hit an IOException, we report it.
    */
    if (exception != null) {
      assert(ch == NonblockingInputStreamWrapper.READ_TIMEOUT)
      var toBeThrown = exception
      // TODO - why would be throwing null?  Seems odd in the original impl
      if(!isPeek)
        exception = null
      throw toBeThrown
    }
  }

  /**
   * Attempts to read a character from the input stream for a specific
   * period of time.
   * @param timeout The amount of time to wait for the character
   * @return The character read, -1 if EOF is reached, or -2 if the
   *   read timed out.
   * @throws IOException
   */
  private def read(timeout: Long, isPeek: Boolean): Int = synchronized {
    checkInputAndThrowIfError(isPeek)
    var to = timeout

    /*
     * If there was a pending character from the thread, then
     * we send it. If the timeout is 0L or the thread was shut down
     * then do a local read.
     */
    if (ch >= -1) {
      assert(exception == null)
    }
    else if ((to == 0L || isShutdown) && !threadIsReading) {
      ch = in.read()
    }
    else {
      /*
       * If the thread isn't reading already, then ask it to do so.
       */
      if (!threadIsReading) {
        threadIsReading = true
        notify()
      }
      var isInfinite = (timeout <= 0L)
      import scala.util.control.Breaks._
      /*
       * So the thread is currently doing the reading for us. So
       * now we play the waiting game.
       */
      breakable(while (isInfinite || to > 0L)  {
        var start = System.currentTimeMillis()

        try this.wait(to)
        catch {
          case e: InterruptedException =>
          /* IGNORED */
        }
        checkInputAndThrowIfError(isPeek)

        if (ch >= -1) {
          assert(exception == null)
          break
        }

        if (!isInfinite) {
          to -= System.currentTimeMillis() - start
        }
      })
    }

    /*
     * ch is the character that was just read. Either we set it because
     * a local read was performed or the read thread set it (or failed to
     * change it).  We will return it's value, but if this was a peek
     * operation, then we leave it in place.
     */
    var ret = ch
    if (!isPeek) {
      ch = NonblockingInputStreamWrapper.READ_TIMEOUT
    }
    ret
  }

  /**
   * This version of read() is very specific to jline's purposes, it
   * will always always return a single byte at a time, rather than filling
   * the entire buffer.
   */
  override def read (b: Array[Byte], off: Int, len: Int): Int = {
    if (b == null) throw new NullPointerException()
    else if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException()
    else if (len == 0) 0
    else {
      var c: Int = this.read(0L)
      if (c == -1) -1
      else {
        b(off) = c.toByte
        1
      }
    }
  }

  def run (): Unit = {
    //Log.debug("NonBlockingInputStream start")
    var needToShutdown = false
    var needToRead = false
    while (!needToShutdown) {
      /*
       * Synchronize to grab variables accessed by both this thread
       * and the accessing thread.
       */
      this.synchronized {
        needToShutdown = this.isShutdown
        needToRead     = this.threadIsReading
        try {
          /*
           * Nothing to do? Then wait.
           */
          if (!needToShutdown && !needToRead) wait(0)
        } catch  {
          case e: InterruptedException => /* IGNORED */
        }
      }

      /*
       * We're not shutting down, but we need to read. This cannot
       * happen while we are holding the lock (which we aren't now).
       */
      if (!needToShutdown && needToRead) {
        var charRead = NonblockingInputStreamWrapper.READ_TIMEOUT
        var failure: IOException = null
        try {
          charRead = in.read()
        } catch {
          case e: IOException =>
            failure = e
        }
        /*
         * Re-grab the lock to update the state.
         */
        this.synchronized {
          exception = failure
          ch = charRead
          threadIsReading = false
          notify()
        }
      }
    }
    //Log.debug("NonBlockingInputStream shutdown")
  }
}
