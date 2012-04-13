package com.minesnap.dcpu.assembler;

public class UnresolvedOffset extends UnresolvedData {
    private final int offset;

    public UnresolvedOffset(Token token, String reference, int offset) {
        super(token, reference);
        this.offset = offset;
    }

    @Override
    public int getUnresolvedWord() {
        return super.getUnresolvedWord() + offset;
    }
}
