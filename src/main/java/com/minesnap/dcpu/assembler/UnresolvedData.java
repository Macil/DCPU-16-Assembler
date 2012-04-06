package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

// Contains either a 16-bit word of data, or a symbol reference.
public class UnresolvedData implements Resolvable {
    private final int word;
    private final String reference;

    public UnresolvedData(int word) {
        if((word & 0xffff) != word)
            throw new IllegalArgumentException("Word must fit in 16 bits");

        this.word = word;
        this.reference = null;
    }

    public UnresolvedData(String reference) {
        this.word = 0;
        this.reference = reference.toUpperCase();
    }

    public int getUnresolvedWord() {
        return word;
    }

    public String getReference() {
        return reference;
    }

    public int evaluate(Map<String, Integer> refmap)
        throws SymbolLookupError {
        if(reference == null)
            return word;
        Integer ref = refmap.get(reference);
        if(ref == null)
            throw new SymbolLookupError(reference);
        return ref;
    }

    // Note that this isn't accurate for instances owned by Value
    // objects.
    @Override
    public int wordCount() {
        return 1;
    }

    @Override
    public void writeTo(OutputStream out, Map<String, Integer> refmap)
        throws SymbolLookupError, IOException {
        int resolved = evaluate(refmap);
        out.write(resolved & 0x00ff);
        out.write((resolved & 0xff00) >> 8);
    }
}
