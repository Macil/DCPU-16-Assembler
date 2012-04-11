package com.minesnap.dcpu.assembler;

public class IllegalIncludeException extends CompileError {
    public IllegalIncludeException(String message) {
        super(message);
    }
}
