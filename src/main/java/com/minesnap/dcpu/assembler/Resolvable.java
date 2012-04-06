package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Resolvable {
    public int wordCount();
    public void writeTo(OutputStream out, Map<String, Integer> refmap)
        throws SymbolLookupError, IOException;
}
