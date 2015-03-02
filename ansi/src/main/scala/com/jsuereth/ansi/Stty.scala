package com.jsuereth.ansi

import java.lang

import scala.sys.process._
/**
 * A wrapper around simple stty commands we use in UnixTerminals to make nice UIs.
 */
object Stty {

  // TODO - this is the executable we call for STTY.  we may want to allow this to be configured or looked up.
  private val STTY = "stty"
  private val SHELL = "/bin/sh"


  /** disables echoing characters as they are typed, if this system supports STTY. */
  def disableEcho(): Unit = exec("-echo")
  /** Enables echoing characters as they are typed, if this system supports STTY. */
  def enableEcho(): Unit = exec("echo")


  /** Configures the console to buffer by character. */
  def bufferByCharacter(): Unit = exec("-icanon min 1")


  /** Executes Stty command with given argument string. */
  def exec(args: String): Unit = {
    val cmd = s"$STTY $args < /dev/tty"
    //val logger = ProcessLogger(System.out.println, System.err.println)
    val p = Runtime.getRuntime.exec(Array(s"$SHELL", "-c", cmd))
    p.waitFor() match {
      case 0 => ()
      case n =>
        // TODO - Figure out what to do here...
        throw new UnsupportedOperationException(s"It appears your environment does not support STTY, tried to call [$cmd], return value $n")
    }
  }
}
