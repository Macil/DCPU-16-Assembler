package com.minesnap.dcpu.assembler;

import java.io.File;

// Represents a quoted string.

public class StringToken extends Token {
    private final String value;

    public StringToken(String text, String value,
                       File sourceDir, String sourceFile, int lineNumber) {
        super(text, sourceDir, sourceFile, lineNumber);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
