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
        @Override
        public String toString() {
            String content;
            if(resolvable != null)
                content = resolvable.toString();
            else
                content = label;
            return Integer.toString(position)+": "+content;
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
        throws TokenCompileError {
        Map<String, Integer> labelValues = getLabelValues();

        boolean needDoOver = false;
        int newWordPosition = 0;
        for(PositionedResolvable pr : resolvablesList) {
            int oldWordPosition = pr.getPosition();
            pr.setPosition(newWordPosition);
            if(oldWordPosition != newWordPosition)
                needDoOver = true;
            // Don't break now for the do-over immediately because we
            // still want to update the positions so that the labels
            // are more correct.

            Resolvable r = pr.getResolvable();
            if(r != null) {
                r.evaluateLabels(labelValues, newWordPosition);
                newWordPosition += r.wordCount();
            }
        }
        
        // Did some instructions get moved during this last loop? If
        // so, we need to repeat this until the final positions are
        // the same as their values were when we first ran
        // updateLabelValues().
        if(needDoOver) {
            wordPosition = newWordPosition;
            evaluateRefs();
        }
    }

    public void prepare()
        throws TokenCompileError {
        evaluateRefs();
        prepared = true;
    }

    public void writeTo(WordWriter out)
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
