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
    private boolean prepared = false;

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
        prepared = false;

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

        prepared = false;

        return true;
    }

    private Map<String, Integer> getLabelValues() {
        Map<String, Integer> labelValues = new HashMap<String, Integer>();

        for(Map.Entry<String, Integer> labelIndex : labelIndexes.entrySet()) {
            String label = labelIndex.getKey();
            int index = labelIndex.getValue();

            int position = resolvablesList.get(index).getPosition();
            labelValues.put(label, position);
        }

        return labelValues;
    }

    private void evaluateRefs()
        throws SymbolLookupError {
        Map<String, Integer> labelValues = getLabelValues();

        int newWordPosition = 0;
        for(PositionedResolvable pr : resolvablesList) {
            pr.setPosition(newWordPosition);

            Resolvable r = pr.getResolvable();
            if(r != null) {
                r.evaluateLabels(labelValues);
                newWordPosition += r.wordCount();
            }
        }
        
        // Did we come up with more words than last time? If so, some
        // of the labels may have different values now, so we need to
        // repeat this until our final wordPosition is the same as it
        // was when we first ran updateLabelValues().
        if(newWordPosition != wordPosition) {
            // Note that we started out assuming the best case
            // scenario (all resolvables return the smallest possible
            // wordCount() before the labels are evaluated), so the
            // final wordPosition value can only get larger. This
            // means we don't have to worry at all about the
            // possibility that some resolvables got smaller and
            // cancelled out with the ones that got bigger and we
            // deceptively ended up with the same wordPosition value
            // but changed some of the label values.
            assert(newWordPosition > wordPosition);

            wordPosition = newWordPosition;
            evaluateRefs();
        }
    }

    public void prepare()
        throws SymbolLookupError {
        evaluateRefs();
        prepared = true;
    }

    public void writeTo(OutputStream out)
        throws IOException {
        if(!prepared)
            throw new IllegalStateException("prepare method must be called before writeTo");
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
        prepared = false;
    }
}
