package com.minesnap.dcpu.assembler;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import java.io.OutputStream;

public class ResolverList {
    private int wordPosition = 0;
    private List<PositionedResolvable> resolvablesList = new ArrayList<PositionedResolvable>();
    private Map<String, Integer> labelIndexes = new HashMap<String, Integer>();

    // Only guaranteed to be accurate after updateLabelValues() has been run
    private Map<String, Integer> labelValues = new HashMap<String, Integer>();

    private static class PositionedResolvable {
        private final Resolvable resolvable;
        private final String label;
        private int position;

        public PositionedResolvable(Resolvable resolvable, int position) {
            this.resolvable = resolvable;
            this.label = null;
            this.position = position;
        }

        public PositionedResolvable(String label, int position) {
            this.resolvable = null;
            this.label = label;
            this.position = position;
        }

        public Resolvable getResolvable() {
            return resolvable;
        }
        public String getLabel() {
            return label;
        }
        public int getPosition() {
            return position;
        }
        public void setPosition(int position) {
            this.position = position;
        }
    }

    public ResolverList() {
    }

    public boolean add(Resolvable resolvable) {
        int thisIndex = resolvablesList.size();
        PositionedResolvable pr = new PositionedResolvable(resolvable, wordPosition);
        resolvablesList.add(pr);
        assert(resolvablesList.get(thisIndex) == pr);

        wordPosition += resolvable.wordCount();

        return true;
    }

    public boolean addLabel(String label)
        throws LabelAlreadyExistsError {
        if(labelIndexes.containsKey(label))
            throw new LabelAlreadyExistsError(label);

        int thisIndex = resolvablesList.size();
        PositionedResolvable pr = new PositionedResolvable(label, wordPosition);
        resolvablesList.add(pr);
        assert(resolvablesList.get(thisIndex) == pr);

        labelIndexes.put(label, thisIndex);

        return true;
    }

    private void updateLabelValues() {
        for(Map.Entry<String, Integer> labelIndex : labelIndexes.entrySet()) {
            String label = labelIndex.getKey();
            int index = labelIndex.getValue();

            int position = resolvablesList.get(index).getPosition();
            labelValues.put(label, position);
        }
    }

    private void evaluateRefs() {
        updateLabelValues();

        int newWordPosition = 0;
        for(PositionedResolvable pr : resolvablesList) {
            pr.setPosition(newWordPosition);

            Resolvable r = pr.getResolvable();
            if(r != null) {
                r.evaluateLabels(labelValues);
                newWordPosition += r.wordCount();
            }
        }

        if(newWordPosition != wordPosition) {
            assert(newWordPosition > wordPosition);
            wordPosition = newWordPosition;
            // Some of those labels we just wrote out may be incorrect
            // now. Repeat until the size stabilizes.
            evaluateRefs();
        }
    }

    public void writeTo(OutputStream out)
        throws SymbolLookupError, IOException {
        evaluateRefs();
        for(PositionedResolvable pr : resolvablesList) {
            Resolvable r = pr.getResolvable();
            if(r != null)
                r.writeTo(out);
        }
    }

    public boolean addAll(Collection<? extends Resolvable> c) {
        boolean changed = false;
        for(Resolvable r : c) {
            if(add(r))
                changed = true;
        }
        return changed;
    }
    public void clear() {
        wordPosition = 0;
        resolvablesList.clear();
        labelIndexes.clear();
        labelValues.clear();
    }
}
