package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UnresolvedMultiData implements Resolvable {
    private final List<UnresolvedData> dataList;
    private final int repeatCount;
    private int lastKnownWordCount;

    public UnresolvedMultiData(List<UnresolvedData> dataList) {
        this(dataList, 1);
    }

    public UnresolvedMultiData(List<UnresolvedData> dataList, int repeatCount) {
        this.dataList = dataList;
        this.repeatCount = repeatCount;

        lastKnownWordCount = 0;
        for(UnresolvedData data : dataList) {
            lastKnownWordCount += data.wordCount();
        }
    }

    @Override
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws SymbolLookupError {
        lastKnownWordCount = 0;
        for(UnresolvedData data : dataList) {
            data.evaluateLabels(labelValues, position+lastKnownWordCount);
            lastKnownWordCount += data.wordCount();
        }
    }

    @Override
    public int wordCount() {
        return lastKnownWordCount * repeatCount;
    }

    @Override
    public void writeTo(WordWriter out)
        throws IOException {
        for(int i=0; i<repeatCount; i++) {
            for(UnresolvedData data : dataList) {
                data.writeTo(out);
            }
        }
    }
}
