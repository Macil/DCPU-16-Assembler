package com.minesnap.dcpu.assembler;

public class TokenCompileError extends CompileError {
    private final Token token;

    public TokenCompileError(String message, Token token) {
        super(message+": "+token);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
