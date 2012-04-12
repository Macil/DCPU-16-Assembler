package com.minesnap.dcpu.assembler;

public class TokenizeError extends CompileError {
    private final String sourceFile;
    private final int lineNumber;

    public TokenizeError(String message, String sourceFile, int lineNumber) {
        super(message+": "+sourceFile+":"+lineNumber);
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
