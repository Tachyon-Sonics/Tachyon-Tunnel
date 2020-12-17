package ch.tachyon.tunnel.demo.plugin.timestretch;

import java.util.*;

/**
 * A Binary max-Heap on a float array, that manipulates an int array
 * of indexes refering the float array, instead of the float array itself.
 * The float array itself is hence never modified.
 */
public class FloatIndexBinaryMaxHeap {

    private float[] array;
    private int[] queue;
    private int size = 0;


    public FloatIndexBinaryMaxHeap(float[] array) {
        this.array = array;
        this.queue = new int[array.length];
    }

    public void setBackingArray(float[] array) {
        this.array = array;
    }

    /**
     * Compare the element at index i1 with the element at index i2
     * Indexing + descending order
     */
    private int compare(int i1, int i2) {
        return Float.compare(array[i2], array[i1]);

    }

    private void grow(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = ((oldCapacity < 64) ? ((oldCapacity + 1) * 2) : ((oldCapacity / 2) * 3));
        if (newCapacity < 0) // overflow
            newCapacity = Integer.MAX_VALUE;
        if (newCapacity < minCapacity)
            newCapacity = minCapacity;
        queue = Arrays.copyOf(queue, newCapacity);
    }

    public boolean add(int index) {
        int i = size;
        if (i >= queue.length)
            grow(i + 1);
        size = i + 1;
        if (i == 0) {
            queue[0] = index;
        } else {
            siftUp(i, index);
        }
        return true;
    }

    /**
     * Get the smallest element of the heap without removing it
     * @return the index of the smallest element in the array of values
     */
    public int peek() {
        if (size == 0)
            throw new NoSuchElementException();
        return queue[0];
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    /**
     * Get and remove the smallest element of the heap
     * @return the index of the smallest element in the array of values
     */
    public int poll() {
        if (size == 0)
            throw new NoSuchElementException();
        int s = --size;
        int result = queue[0];
        int x = queue[s];
        if (s != 0)
            siftDown(0, x);
        return result;
    }

    public int getAt(int i) {
        assert i >= 0 && i < size;
        return queue[i];
    }

    public void removeAt(int i) {
        assert i >= 0 && i < size;
        int s = --size;
        if (s != i) { // not removed last element
            int moved = queue[s];
            siftDown(i, moved);
            if (queue[i] == moved)
                siftUp(i, moved);
        }
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it is greater than or equal to its parent, or is the root.
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, int x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            int e = queue[parent];
            if (compare(x, e) >= 0)
                break;
            queue[k] = e;
            k = parent;
        }
        queue[k] = x;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by demoting x down the tree repeatedly until it is less than or equal to its children or is a leaf.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, int x) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            int c = queue[child];
            int right = child + 1;
            if (right < size && compare(c, queue[right]) > 0)
                c = queue[child = right];
            if (compare(x, c) <= 0)
                break;
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(queue, size));
    }

}
