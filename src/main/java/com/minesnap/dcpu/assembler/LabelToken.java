package com.minesnap.dcpu.assembler;

import java.io.File;

public class LabelToken extends Token {
    private final String name;

    public LabelToken(String text, String name,
                      File sourceDir, String sourceFile, int lineNumber) {
        super(text, sourceDir, sourceFile, lineNumber);
        this.name = name;
    }

    // Shouldn't include the colon
    public String getName() {
        return name;
    }
}
