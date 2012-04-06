package com.minesnap.dcpu.assembler;

public class TokenCompileError extends CompileError {
    private final Token token;

    public TokenCompileError(String message, Token token) {
        super(message);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String getMessage() {
        return super.getMessage()+": "+token;
    }
}
