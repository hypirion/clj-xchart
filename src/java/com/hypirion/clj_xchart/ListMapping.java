package com.hypirion.clj_xchart;

import clojure.lang.IFn;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Collection;

// ListMapping is an immutable view over an immutable/persistent collection. It
// consumes no more space than the original collection itself, and henceforth
// can be useful to avoid heavy memory usage.
public class ListMapping implements List<Object> {
    private final List l;
    private final IFn fn;
    public ListMapping(List l, IFn fn){
        this.l = l;
        this.fn = fn;
    }

    public boolean add(Object e) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public List<Object> subList(int from, int to) {
        return new ListMapping(l.subList(from, to), fn);
    }

    public ListIterator<Object> listIterator(int start) {
        return new ListMappingIterator(fn, l.listIterator(start));
    }
    
    public ListIterator<Object> listIterator() {
        return new ListMappingIterator(fn, l.listIterator());
    }

    public ListIterator<Object> iterator() {
        return listIterator();
    }

    public int size() {
        return l.size();
    }

    public int lastIndexOf(Object e) {
        for (int loc = size()-1; loc >= 0; loc--) {
            if (e.equals(get(loc))) {
                return loc;
            }
        }
        return -1;
    }

    public boolean contains(Object e) {
        return indexOf(e) >= 0;
    }

    public boolean isEmpty() {
        return l.isEmpty();
    }

    public int indexOf(Object e) {
        int loc = 0;
        for (Object element : this) {
            if (e.equals(element)) {
                return loc;
            }
            loc++;
        }
        return -1;
    }

    public Object get(int i) {
        return fn.invoke(l.get(i));
    }

    public Object remove(int loc) {
        throw new UnsupportedOperationException();
    }
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public Object set(int loc, Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    public boolean addAll(int i, Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    public boolean addAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    public boolean containsAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        while (e.hasNext()){
            if (!contains(e.next())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size()){
            return (T[]) Arrays.copyOf(toArray(), size(), a.getClass());
        }
        System.arraycopy(toArray(), 0, a, 0, size());
        if (a.length > size())
            a[size()] = null;
        return a;
    }
    public Object[] toArray() {
        Object[] arr = l.toArray();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = fn.invoke(arr[i]);
        }
        return arr;
    }
}
