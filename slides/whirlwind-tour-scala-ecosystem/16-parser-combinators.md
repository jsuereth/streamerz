# Parser Combinators

```scala
class SimpleParser extends RegexParsers {
  def word: Parser[String] = 
    "[a-z]+".r ^^ { _.toString }
  def number: Parser[Int] =
    "(0|\[1-9]\\d*)".r ^^ { _.toInt }
  def freq: Parser[WordFreq] =
    word ~ number ^^ {
      case wd ~ fr => WordFreq(wd,fr)
    }
}
```