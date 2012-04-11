package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.util.Map;

public class UnresolvedOffset extends UnresolvedData {
    private final int offset;

    public UnresolvedOffset(String reference, int offset) {
        super(reference);
        this.offset = offset;
    }

    @Override
    public int getUnresolvedWord() {
        int word = super.getUnresolvedWord();
        return word + offset;
    }

    @Override
    public void writeTo(WordWriter out)
        throws IOException {
        assert(checkResolved());
        out.writeWord(getUnresolvedWord());
    }
}
