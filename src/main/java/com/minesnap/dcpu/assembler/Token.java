package com.minesnap.dcpu.assembler;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

public class Token {
    private final String value;
    private final String sourceFile;
    private final int lineNumber;

    public Token(String value, String sourceFile, int lineNumber) {
        this.value = value;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public String getValue() {
        return value;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String toString() {
        return "Token(\""+escapeJava(value)+"\", "+sourceFile+", "+lineNumber+")";
    }
}
