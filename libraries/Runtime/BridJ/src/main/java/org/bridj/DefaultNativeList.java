/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import static org.bridj.Pointer.*;

/**
 * TODO : smart rewrite by chunks for removeAll and retainAll !
 * @author ochafik
 * @param <T> 
 */
class DefaultNativeList<T> extends AbstractList<T> implements NativeList<T>, RandomAccess {
    /*
     * For optimization purposes, please look at AbstractList.java and AbstractCollection.java :
     * http://www.koders.com/java/fidCFCB47A1819AB345234CC04B6A1EA7554C2C17C0.aspx?s=iso
     * http://www.koders.com/java/fidA34BB0789922998CD34313EE49D61B06851A4397.aspx?s=iso
     * 
     * We've reimplemented more methods than needed on purpose, for performance reasons (mainly using a native-optimized indexOf, that uses memmem and avoids deserializing too many elements)
     */
    
    final ListType type;
    final PointerIO<T> io;
    volatile Pointer<T> pointer;
    volatile long size;

    public Pointer<?> getPointer() {
        return pointer;
    }
    
    /**
     * Create a native list that uses the provided storage and implementation strategy
     * @param pointer
     * @param type Implementation type
     */
    DefaultNativeList(Pointer<T> pointer, ListType type) {
        if (pointer == null || type == null)
            throw new IllegalArgumentException("Cannot build a " + getClass().getSimpleName() + " with " + pointer + " and " + type);
        
        this.io = pointer.getIO("Cannot create a list out of untyped pointer " + pointer);
        this.type = type;
        this.size = pointer.getValidElements();
        this.pointer = pointer;
    }
    
    protected void checkModifiable() {
        if (type == ListType.Unmodifiable)
            throw new UnsupportedOperationException("This list is unmodifiable");
    }
    
    protected int safelyCastLongToInt(long i, String content) {
        if (i > Integer.MAX_VALUE)
            throw new RuntimeException(content + " is bigger than Java int's maximum value : " + i);
        
        return (int)i;
    }
    
    @Override
    public int size() {
        return safelyCastLongToInt(size, "Size of the native list");
    }
    
    @Override
    public void clear() {
        checkModifiable();
        size = 0;
    }

    @Override
    public T get(int i) {
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        
        return pointer.get(i);
    }

    @Override
    public T set(int i, T e) {
        checkModifiable();
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        
        T old = pointer.get(i);
        pointer.set(i, e);
        return old;
    }
    
    void add(long i, T e) {
        checkModifiable();
        if (i > size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        requireSize(size + 1);
        if (i < size) {
            pointer.moveBytesAtOffsetTo(i, pointer, i + 1, size - i);
        }
        pointer.set(i, e);
        size++;
    }
    @Override
    public void add(int i, T e) {
        add((long)i, e);
    }

    protected void requireSize(long newSize) {
        if (newSize > pointer.getValidElements()) {
            switch (type) {
                case Dynamic:
                    long nextSize = newSize < 5 ? newSize + 1 : (long)(newSize * 1.6);
                    Pointer<T> newPointer = allocateArray(io, nextSize);
                    pointer.copyTo(newPointer);
                    pointer = newPointer;
                    break;
                case FixedCapacity:
                    throw new UnsupportedOperationException("This list has a fixed capacity, cannot grow its storage");
                case Unmodifiable:
                    // should not happen !
                    checkModifiable();
            }
        }
    }
    
    T remove(long i) {
        checkModifiable();
        if (i >= size || i < 0)
            throw new IndexOutOfBoundsException("Invalid index : " + i + " (list has size " + size +")");
        T old = pointer.get(i);
        long targetSize = io.getTargetSize();
        pointer.moveBytesAtOffsetTo((i + 1) * targetSize, pointer, i * targetSize, targetSize);
        size--;
        return old;
    }

    @Override
    public T remove(int i) {
        return remove((long)i);
    }
    
    @Override
    public boolean remove(Object o) {
        checkModifiable();
        long i = indexOf(o, true, 0);
        if (i < 0)
            return false;
        
        remove(i);
        return true;
    }

    long indexOf(Object o, boolean last, int offset) {
        Pointer<T> pointer = this.pointer;
        assert offset >= 0 && (last || offset > 0);
        if (offset > 0)
            pointer = pointer.next(offset);
        
        Pointer<T> needle = allocate(io);
        needle.set((T)o);
        Pointer<T> occurrence = last ? pointer.findLast(needle) : pointer.find(needle);
        if (occurrence == null)
            return -1;
        
        return occurrence.getPeer() - pointer.getPeer();
    }
    
    @Override
    public int indexOf(Object o) {
        return safelyCastLongToInt(indexOf(o, false, 0), "Index of the object");
    }

    @Override
    public int lastIndexOf(Object o) {
        return safelyCastLongToInt(indexOf(o, true, 0), "Last index of the object");
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }
    
    @Override
    public boolean addAll(int i, Collection<? extends T> clctn) {
        if (i >= 0 && i < size)
            requireSize(size + clctn.size());
        return super.addAll(i, clctn);
    }
    
    @Override
    public Object[] toArray() {
        return pointer.validElements(size).toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return pointer.validElements(size).toArray(ts);
    }
}
