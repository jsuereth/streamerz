package com.jsuereth.ansi.markdown

import org.parboiled.Parboiled
import org.pegdown.plugins.{InlinePluginParser, PegDownPlugins}
import org.pegdown.{Extensions, Parser}

/**
 * Converts markdown document into ANSI terminal codes.
 */
object MarkdownToAnsi {
  private val DEFAULT_MAX_PARSING_TIME = 2000L
  def makeParser =
    Parboiled.createParser[Parser, AnyRef](
      classOf[Parser],
      Integer.valueOf(Extensions.AUTOLINKS),
      java.lang.Long.valueOf(DEFAULT_MAX_PARSING_TIME),
      Parser.DefaultParseRunnerProvider,
      PegDownPlugins.builder().withPlugin(classOf[AnsiColorPluginParser]).build())
  def convert(markdown: String): String = {
    val p = makeParser
    val ast = p.parse(markdown.toCharArray)
    val v = new AnsiSerializer
    v.toAnsiString(ast)
  }


  def test: String =
    convert(
     """[test link](http://test.com)""")+
    convert(
     """|
        |Hello, **world**
        |
        |*How* are you?
        |
        |```scala
        |val x = "Hi"
        |```
        |
        |*Hello*
        | * Some
        | * things
        | * to
        | * show
      """.stripMargin)

  // lame unit tests.
  def main(args: Array[String]): Unit = {
    System.err.println(convert("This is a [test link](http://test.com)"))
    System.err.println(convert("Here is a http://rawlink.com"))
    System.err.println(convert("*Italic*"))
    System.err.println(convert("**Bold**"))
    System.err.println(convert("_Underline_"))
    System.err.println(convert("Some `example` code"))
    System.err.println(convert(
      """* A sample
        |* Unordered
        |* List
      """.stripMargin))
    System.err.println(convert(
      """```scala
        |  val x = "Hi"
        |  def foo(x: Int) = println(x)
        |```
      """.stripMargin))
    System.err.println(convert(
      """Test
        |
        |# Whirlwind Tour
        |## of the %red%Scala %reset%Ecosystem
      """.stripMargin))
  }
}