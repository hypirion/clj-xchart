package com.hypirion.clj_xchart;

import clojure.lang.IFn;
import java.util.ListIterator;

public class ListMappingIterator implements ListIterator<Object> {
    IFn fn;
    ListIterator iter;

    ListMappingIterator(IFn fn, ListIterator iter) {
        this.fn = fn;
        this.iter = iter;
    }

    public void add(Object e) {
        throw new UnsupportedOperationException("ListMappingIterator cannot add elements");
    }

    public void remove() {
        throw new UnsupportedOperationException("ListMappingIterator cannot remove elements");
    }

    public void set(Object e) {
        throw new UnsupportedOperationException("ListMappingIterator cannot set elements");
    }

    public boolean hasNext() {
        return iter.hasNext();
    }
    public boolean hasPrevious() {
        return iter.hasPrevious();
    }
    public Object next() {
        return fn.invoke(iter.next());
    }
    public Object previous() {
        return fn.invoke(iter.previous());
    }
    public int nextIndex() {
        return iter.nextIndex();
    }
    public int previousIndex() {
        return iter.previousIndex();
    }
}
