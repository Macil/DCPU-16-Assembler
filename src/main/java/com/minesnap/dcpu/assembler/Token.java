package com.minesnap.dcpu.assembler;

import java.io.File;

public class Token {
    private final String text;
    private final File sourceDir;
    private final String sourceFile;
    private final int lineNumber;

    public Token(String text, File sourceDir, String sourceFile, int lineNumber) {
        this.text = text;
        this.sourceDir = sourceDir;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public String getText() {
        return text;
    }

    public File getSourceDir() {
        return sourceDir;
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
