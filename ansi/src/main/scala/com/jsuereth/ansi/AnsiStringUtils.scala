package com.jsuereth.ansi


case class AnsiEscapeLocation(idx: Int, charLength: Int)

/**
 * Utilities for dealing with ANSI strings.  Adapted from sbt's ConsoleLogger.
 */
object AnsiStringUtils {
  /** Escape character, used to introduce an escape sequence. */
  private final val ESC = '\u001B'

  /**
   * An escape terminator is a character in the range `@` (decimal value 64) to `~` (decimal value 126).
   * It is the final character in an escape sequence.
   *
   * cf. http://en.wikipedia.org/wiki/ANSI_escape_code#CSI_codes
   */
  private def isEscapeTerminator(c: Char): Boolean = c >= '@' && c <= '~'

  /**
   * Test if the character AFTER an ESC is the ANSI CSI.
   *
   * see: http://en.wikipedia.org/wiki/ANSI_escape_code
   *
   * The CSI (control sequence instruction) codes start with ESC + '['.   This is for testing the second character.
   *
   * There is an additional CSI (one character) that we could test for, but is not frequently used, and we don't
   * check for it.
   *
   * cf. http://en.wikipedia.org/wiki/ANSI_escape_code#CSI_codes
   */
  private def isCSI(c: Char): Boolean = c == '['

  /**
   * Tests whether or not a character needs to immediately terminate the ANSI sequence.
   *
   * c.f. http://en.wikipedia.org/wiki/ANSI_escape_code#Sequence_elements
   */
  private def isAnsiTwoCharacterTerminator(c: Char): Boolean = (c >= '@') && (c <= '_')

  /**
   * Returns the string `s` with escape sequences removed.
   * An escape sequence starts with the ESC character (decimal value 27) and ends with an escape terminator.
   *
   * Note: Some sequences may be longer than two characters.
   */
  def removeEscapeSequences(s: String): String =
    if (s.isEmpty) s
    else {
      val sb = new java.lang.StringBuilder
      def removeEscapes(idx: Int): Unit =
        nextEscapeSequence(s, idx) match {
          case None =>  sb.append(s.substring(idx))
          case Some(AnsiEscapeLocation(startIdx, length)) =>
            sb.append(s.substring(idx, startIdx))
            removeEscapes(startIdx + length)
        }
      removeEscapes(0)
      sb.toString
    }

  /** Returns all the ansi escape locations for a given string. */
  def delineateEscapes(s: String): Seq[AnsiEscapeLocation] = {
    val buf = Vector.newBuilder[AnsiEscapeLocation]
    def findEscape(idx: Int): Unit =
      nextEscapeSequence(s, idx) match {
        case Some(e) =>
          buf += e
          findEscape(e.idx + e.charLength + 1)
        case None => ()
      }
    findEscape(0)
    buf.result()
  }

  /** Calcuulates the real size of an ANSI string. */
  def realLength(s: String): Int =
    s.length - delineateEscapes(s).map(_.charLength).sum

  /** Truncates an ANSI string (preserving escape sequences). */
  def truncate(s: String, maxLength: Int): String = {
    val escapes = delineateEscapes(s)
    val realLength = s.length - escapes.map(_.charLength).sum
    if(realLength < maxLength) s
    else {
      // Here we truncate....
      def truncateLocation(idx: Int, currentSize: Int, remainingEscapes: Seq[AnsiEscapeLocation]): Int =
        if(currentSize >= maxLength) idx
        else {
          remainingEscapes match {
            case Seq(head, tail @ _*) =>
              val maxInc = idx + (maxLength - currentSize)
              if(maxInc > head.idx) {
                // We need to include the escape in the truncated string.
                val extraLength = (head.idx - idx)
                truncateLocation(head.idx + head.charLength, currentSize + extraLength, tail)
              } else maxInc
            case Nil => idx + (maxLength - currentSize)
          }
        }
      val idx = truncateLocation(0, 0, escapes)
      s.substring(0, idx)
    }
  }

  /**
   * Looks for the next position in a string which contains an escape sequence.
   * @param s The string we are searching.
   * @param start The starting index
   * @return  None, if no sequence remains.  Some((start, length)) index if it odes.
   */
  def nextEscapeSequence(s: String, start: Int): Option[AnsiEscapeLocation] = {
    val escIndex = s.indexOf(ESC, start)
    if(escIndex < 0) None
    else {
      if(s.length < (escIndex + 2)) Some(AnsiEscapeLocation(escIndex,1))
      else {
        val nextChar = s.charAt(escIndex+1)
        if(isCSI(nextChar)) findEscapeTerminator(s, escIndex, skip = 2)
        else if(isAnsiTwoCharacterTerminator(nextChar)) Some(AnsiEscapeLocation(escIndex, 2))
        else {
          // Some non-ANSI escape sequence may be in use, and we should try to handle it.
          findEscapeTerminator(s, escIndex, skip=1)
        }
      }
    }
  }

  /**
   * Looks for the full CSI ANSI escape sequence.
   * @param s  The underlying string.
   * @param escIndex  THe start of the escape sequence (the ESC character)
   * @return The escape location, if this is a complete escape.
   */
  private def findEscapeTerminator(s: String, escIndex: Int, skip: Int): Option[AnsiEscapeLocation] = {
    var idx= escIndex + skip
    while(idx < s.length) {
      if(isEscapeTerminator(s.charAt(idx))) return Some(AnsiEscapeLocation(escIndex, (idx - escIndex)+1))
      idx += 1
    }
    None
  }
}
