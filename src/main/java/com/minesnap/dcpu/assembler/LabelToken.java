package com.minesnap.dcpu.assembler;

public class LabelToken extends Token {
    private final String name;

    public LabelToken(String text, String name,
                       String sourceFile, int lineNumber) {
        super(text, sourceFile, lineNumber);
        this.name = name;
    }

    // Shouldn't include the colon
    public String getName() {
        return name;
    }
}
