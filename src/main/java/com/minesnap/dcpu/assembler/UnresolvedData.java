package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.util.Map;
import java.util.List;

// Contains either a 16-bit word of data, or a symbol reference.
public class UnresolvedData implements Resolvable {
    private final Token token;
    private Integer word;
    private final String reference;

    public UnresolvedData(Token token, int word) {
        if((word & 0xffff) != word)
            throw new IllegalArgumentException("Word must fit in 16 bits");

        this.token = token;
        this.word = word;
        this.reference = null;
    }

    public UnresolvedData(Token token, String reference) {
        if(reference == null)
            throw new IllegalArgumentException("Reference may not be null when using reference constructor");

        this.token = token;
        this.word = null;
        this.reference = reference.toUpperCase();
    }

    public Token getToken() {
        return token;
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
        throws TokenCompileError {
        if(reference != null) {
            Integer value = labelValues.get(reference);
            if(value == null)
                throw new SymbolLookupError(reference, getToken());
            this.word = value;
        }

        // Check if this value is in range and will work as a 16-bit
        // value. Note that we call .getUnresolvedWord() so that we
        // get the value after any subclasses have done their part.
        int word = getUnresolvedWord();
        if((word & 0xffff0000) != 0) {
            // Note that we don't allow a negative final value here.
            throw new TokenCompileError("Value doesn't fit in 16 bits", getToken());
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
        // Call getUnresolvedWord() so subclasses can do their thing.
        out.writeWord(getUnresolvedWord());
    }
}
