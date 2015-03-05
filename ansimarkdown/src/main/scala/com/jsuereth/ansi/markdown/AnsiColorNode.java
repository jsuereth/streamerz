package com.jsuereth.ansi.markdown;

import org.pegdown.ast.TextNode;

/**
 * Represents a block of ANSI color text.
 */
public class AnsiColorNode extends TextNode {
    private String colorText;
    public AnsiColorNode(String text) {
        super("");
        colorText = text;
    }

    final public String getColorText() {
        return colorText;
    }
}
