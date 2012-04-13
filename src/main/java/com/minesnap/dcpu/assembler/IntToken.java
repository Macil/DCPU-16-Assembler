package com.minesnap.dcpu.assembler;

import java.io.File;

// Represents a number, possibly prefixed with a negative sign, or a
// character in single quotes.

public class IntToken extends Token {
    private final int value;

    public IntToken(String text, int value,
                    File sourceDir, String sourceFile, int lineNumber) {
        super(text, sourceDir, sourceFile, lineNumber);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
