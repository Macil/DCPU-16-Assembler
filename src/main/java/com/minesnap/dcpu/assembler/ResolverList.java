package com.minesnap.dcpu.assembler;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;

public class ResolverList implements Iterable<Resolvable> {
    private int wordPosition;
    private List<Resolvable> list;

    public ResolverList() {
        wordPosition = 0;
        list = new ArrayList<Resolvable>();
    }

    public int getWordPosition() {
        return wordPosition;
    }

    public boolean add(Resolvable resolvable) {
        wordPosition += resolvable.wordCount();
        return list.add(resolvable);
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
        list.clear();
    }
    public Resolvable get(int index) {
        return list.get(index);
    }
    public ListIterator<Resolvable> listIterator() {
        return Collections.unmodifiableList(list).listIterator();
    }
    @Override
    public Iterator<Resolvable> iterator() {
        return listIterator();
    }
}
