package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Resolvable {
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws TokenCompileError;
    public int wordCount();
    public void writeTo(WordWriter out)
        throws IOException;
}
