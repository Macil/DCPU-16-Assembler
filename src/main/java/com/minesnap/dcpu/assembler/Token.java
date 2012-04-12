package com.minesnap.dcpu.assembler;

public class Token {
    private final String text;
    private final String sourceFile;
    private final int lineNumber;

    public Token(String text, String sourceFile, int lineNumber) {
        this.text = text;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public String getText() {
        return text;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String toString() {
        return "Token(\""+Util.escapeJava(text)+"\", "+sourceFile+", "+lineNumber+")";
    }
}
