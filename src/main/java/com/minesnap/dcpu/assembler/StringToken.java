package com.minesnap.dcpu.assembler;

// Represents a quoted string.

public class StringToken extends Token {
    private final String value;

    public StringToken(String text, String value,
                       String sourceFile, int lineNumber) {
        super(text, sourceFile, lineNumber);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
