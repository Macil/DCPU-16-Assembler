package com.minesnap.dcpu.assembler;

public class SymbolLookupError extends CompileError {
    public SymbolLookupError(String symbol) {
        super("Could not find symbol: "+symbol);
    }
}
