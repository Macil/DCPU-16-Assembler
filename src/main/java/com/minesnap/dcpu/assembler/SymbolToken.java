package com.minesnap.dcpu.assembler;

import java.io.File;

public class SymbolToken extends Token {
    public SymbolToken(String text, File sourceDir, String sourceFile, int lineNumber) {
        super(text, sourceDir, sourceFile, lineNumber);
    }
}
