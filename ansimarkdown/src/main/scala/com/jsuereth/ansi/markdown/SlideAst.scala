package com.jsuereth.ansi.markdown


// TODO - figure out how to convert markdown into something we can render as nice slides.
sealed trait SlideAst
case class Header1(ansiText: String) extends SlideAst
case class Header2(ansiText: String) extends SlideAst
case class Paragraph(ansiText: String) extends SlideAst