package com.minesnap.dcpu.assembler;

public class SymbolLookupError extends TokenCompileError {
    public SymbolLookupError(String symbol, Token token) {
        super("Could not find symbol "+symbol, token);
    }
}
