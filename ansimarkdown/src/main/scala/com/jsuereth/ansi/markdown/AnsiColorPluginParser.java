package com.jsuereth.ansi.markdown;


        import org.parboiled.BaseParser;
        import org.parboiled.Rule;
        import org.parboiled.support.StringBuilderVar;
        import org.pegdown.Parser;
        import org.pegdown.plugins.BlockPluginParser;
        import org.pegdown.plugins.InlinePluginParser;

/** A parser which rips `%<color>%` codes out of markdown, as an extension. */
public class AnsiColorPluginParser extends Parser implements InlinePluginParser {

    public AnsiColorPluginParser() {
        super(ALL, 1000l, DefaultParseRunnerProvider);
    }

    @Override
    public Rule[] inlinePluginRules() {
        return new Rule[] {AnsiColor()};
    }

    public Rule AnsiColor() {
        StringBuilderVar text = new StringBuilderVar();
        return NodeSequence(
                Ch('%'), text.clearContents(),
                OneOrMore(TestNot(Ch('%')), BaseParser.ANY, text.append(matchedChar())),
                push(new AnsiColorNode(text.getString())),
                Ch('%')
        );
    }
}