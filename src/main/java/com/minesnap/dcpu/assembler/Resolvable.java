package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Resolvable {
    public void evaluateLabels(Map<String, Integer> labelValues)
        throws SymbolLookupError;
    public int wordCount();
    public void writeTo(OutputStream out)
        throws IOException;
}
