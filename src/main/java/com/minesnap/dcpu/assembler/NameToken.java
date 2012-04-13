package com.minesnap.dcpu.assembler;

import java.io.File;

// Represents an unquoted string, usually either an instruction,
// label, or register.

public class NameToken extends Token {
    public NameToken(String text, File sourceDir, String sourceFile, int lineNumber) {
        super(text, sourceDir, sourceFile, lineNumber);
    }
}
