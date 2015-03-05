package com.jsuereth.ansi
package markdown

import java.awt.Color

import org.pegdown.ast._

/**
 * A pegodwn serializer which renders ASCII.
 *
 * Currently unfinished.
 */
class AnsiSerializer extends Visitor {
  private val buf = new StringBuilder

  private def link(text: String): String = s"${Ansi.UNDERLINE}${Ansi.BLUE}$text${Ansi.RESET_COLOR}"
  override def visit(node: AbbreviationNode): Unit = ???
  override def visit(node: TextNode): Unit =
    node match {
      case color: AnsiColorNode =>
        color.getColorText match {
          case "red" => buf.append(Ansi.RED)
          case "blue" => buf.append(Ansi.BLUE)
          case "green" => buf.append(Ansi.GREEN)
          case "cyan" => buf.append(Ansi.CYAN)
          case "yellow" => buf.append(Ansi.YELLOW)
          case "magenta" => buf.append(Ansi.MAGENTA)
          case "reset" => buf.append(Ansi.RESET_COLOR)
          case x =>
            System.err.println(s"Unknown ANSI color escape: [$x]")
            // Ignore codes we do not understand, and do not show them.
        }
      case _ =>
        // TODO - filter ansi escapes?
        buf.append(node.getText)
    }
  override def visit(node: WikiLinkNode): Unit = {
    // TODO - unsupported in ANSI, allow or errors?
    buf.append(link(node.getText))
  }
  override def visit(node: VerbatimNode): Unit = {
    buf.append(node.getText)
  }
  override def visit(node: TableRowNode): Unit = ???
  override def visit(node: TableNode): Unit = ???
  override def visit(node: TableHeaderNode): Unit = ???
  override def visit(node: TableColumnNode): Unit = ???
  override def visit(node: TableCellNode): Unit = ???
  override def visit(node: TableCaptionNode): Unit = ???
  override def visit(node: TableBodyNode): Unit = ???
  override def visit(node: StrongEmphSuperNode): Unit = {
    if(node.isClosed) {
      if(node.isStrong) buf.append(Ansi.BOLD)
      else buf.append(Ansi.ITALIC)
      // TODO - Force bold when anyone would reset in the nested calls...
      visitChildren(node)
      buf.append(Ansi.RESET_COLOR)
      // TODO - see if we can keep things as bold in children?
    } else {
      // Treat raw for now...
      buf.append(node.getChars)
      visitChildren(node)
    }
  }
  override def visit(node: AnchorLinkNode): Unit = ???
  override def visit(node: SpecialTextNode): Unit = {
    node.getText match {
      //case "*" => buf.append(s"\n${Ansi.FOREGROUND_COLOR(Color.GRAY)}*${Ansi.RESET_COLOR}")
      case x => buf.append(x) //sys.error(s"Unable to handle special text [$x]")
    }
  }
  override def visit(node: SimpleNode): Unit = ???
  override def visit(node: Node): Unit = ???
  override def visit(node: StrikeNode): Unit = ???
  override def visit(node: HtmlBlockNode): Unit = ???
  override def visit(node: HeaderNode): Unit = {
    node.getLevel match {
      case 1 => buf.append(Ansi.BOLD)
      case _ => buf.append(Ansi.ITALIC)
    }
    visitChildren(node)
    buf.append(Ansi.RESET_COLOR).append("\n")
  }
  override def visit(node: ExpLinkNode): Unit = {
    //System.err.println(s"ExpLinkNode - $node, title = ${node.title}")
    buf.append(Ansi.UNDERLINE).append(Ansi.BLUE)
    visitChildren(node)
    buf.append(Ansi.RESET_COLOR)
  }
  override def visit(node: ExpImageNode): Unit = ???
  override def visit(node: DefinitionTermNode): Unit = ???
  override def visit(node: DefinitionNode): Unit = ???
  override def visit(node: DefinitionListNode): Unit = ???
  override def visit(node: CodeNode): Unit = {
    node.getText match {
      case code if code startsWith "scala" =>
        buf.append(SyntaxHighlighter.ansiHighlight(code.drop(6)))
      case raw =>
        buf.append(s"${Ansi.FOREGROUND_COLOR(Color.GRAY)}$raw${Ansi.RESET_COLOR}")
    }
  }
  override def visit(node: BulletListNode): Unit = {
    // TODO - Put us in bulllet list node mode, and set indent.
    visitChildren(node)
  }
  override def visit(node: BlockQuoteNode): Unit = ???
  override def visit(node: AutoLinkNode): Unit = {
    buf.append(link(node.getText))
  }
  override def visit(node: SuperNode): Unit = visitChildren(node)
  override def visit(node: InlineHtmlNode): Unit = ???
  override def visit(node: ListItemNode): Unit = {
    // TODO - Choose a character based on the nesting level.
    buf.append(s"\n ${Ansi.FOREGROUND_COLOR(Color.GRAY)}*${Ansi.RESET_COLOR} ")
    visitChildren(node)
  }
  override def visit(node: MailLinkNode): Unit = ???
  override def visit(node: OrderedListNode): Unit = {
    // TODO - put us in orderedListNodeMode and set indent.
    visitChildren(node)
  }
  override def visit(node: ParaNode): Unit = {
    visitChildren(node)
    buf.append("\n")
  }  // TODO - Correct to start a new parser.
  override def visit(node: QuotedNode): Unit = ???
  override def visit(node: ReferenceNode): Unit = ???
  override def visit(node: RefImageNode): Unit = ???
  override def visit(node: RefLinkNode): Unit = ???

  override def visit(node: RootNode): Unit = {
    // TODO - visit references/abberviations
    visitChildren(node)
  }

  final def toAnsiString(rootNode: RootNode): String = {
    rootNode.accept(this)
    buf.toString()
  }

  private final def visitChildren(node: SuperNode): Unit = {
    import scala.collection.JavaConverters._
    node.getChildren.asScala foreach (_ accept this)
  }
}
