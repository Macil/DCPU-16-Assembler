package com.minesnap.dcpu.assembler;

// Represents an unquoted string, usually either an instruction,
// label, or register.

public class NameToken extends Token {
    public NameToken(String text, String sourceFile, int lineNumber) {
        super(text, sourceFile, lineNumber);
    }
}
