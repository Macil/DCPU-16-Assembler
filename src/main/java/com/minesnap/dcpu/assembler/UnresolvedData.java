package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.util.Map;
import java.util.List;

// Contains either a 16-bit word of data, or a symbol reference.
public class UnresolvedData implements Resolvable {
    private Integer word;
    private final String reference;

    public UnresolvedData(int word) {
        if((word & 0xffff) != word)
            throw new IllegalArgumentException("Word must fit in 16 bits");

        this.word = word;
        this.reference = null;
    }

    public UnresolvedData(String reference) {
        this.word = null;
        this.reference = reference.toUpperCase();
    }

    // The word may or may not be resolved yet.
    public int getUnresolvedWord() {
        if(word == null)
            return 0;
        return word;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws SymbolLookupError {
        if(reference != null) {
            Integer value = labelValues.get(reference);
            if(value == null)
                throw new SymbolLookupError(reference);
            this.word = value;
        }
    }

    // Note that this isn't accurate for instances owned by Value
    // objects.
    @Override
    public int wordCount() {
        return 1;
    }

    public boolean checkResolved() {
        return word != null;
    }

    @Override
    public void writeTo(WordWriter out)
        throws IOException {
        assert(checkResolved());
        out.writeWord(word);
    }
}
