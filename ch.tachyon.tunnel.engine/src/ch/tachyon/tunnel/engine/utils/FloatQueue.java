package ch.tachyon.tunnel.engine.utils;

/**
 * A bounded FIFO queue for floats (typically audio samples).
 * Implemented as a circular array.
 */
public class FloatQueue {

    private final float[] data;
    private final int capacity;
    private int readPos;
    private int writePos;
    private int size;


    public FloatQueue(int capacity) {
        this(0, capacity);
    }

    public FloatQueue(int initSize, int capacity) {
        if (initSize > capacity)
            throw new IllegalArgumentException();
        this.capacity = capacity;
        this.data = new float[capacity];
        this.writePos = initSize;
        this.readPos = 0;
        this.size = initSize;
    }

    public int getSize() {
        return this.size;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void push(float value) {
        if (size >= capacity)
            throw new IllegalStateException("Buffer overflow");
        data[writePos] = value;
        writePos = (writePos + 1) % capacity;
        size++;
    }

    public void pushArray(float[] values) {
        pushArray(values, values.length);
    }

    public void pushArray(float[] values, int count) {
        pushArray(values, 0, count);
    }

    public void pushArray(float[] values, int offset, int count) {
        if (size + count > capacity)
            throw new IllegalStateException("Buffer overflow");
        size += count;
        while (count > 0){
            int amount = Math.min(count, capacity - writePos);
            System.arraycopy(values, offset, data, writePos, amount);
            writePos = (writePos + amount) % capacity;
            offset += amount;
            count -= amount;
        }
    }

    /**
     * Get the next value without removing it from the queue
     */
    public float peek() {
        if (size == 0)
            throw new IllegalStateException("Buffer underflow");
        return data[readPos];
    }

    /**
     * Get the next value and remove it from the queue
     */
    public float pop() {
        if (size == 0)
            throw new IllegalStateException("Buffer underflow");
        float result = data[readPos];
        size--;
        readPos = (readPos + 1) % capacity;
        return result;
    }

    public void popArray(float[] target) {
        popArray(target, 0, target.length);
    }

    public void popArray(float[] target, int offset, int count) {
        if (size < count)
            throw new IllegalStateException("Buffer underflow");
        size -= count;
        while (count > 0) {
            int amount = Math.min(count, capacity - readPos);
            System.arraycopy(data, readPos, target, offset, amount);
            readPos = (readPos + amount) % capacity;
            offset += amount;
            count -= amount;
        }
    }

    @Override
    public String toString() {
        return "FloatQueue{capacity=" + capacity + ",size=" + size + "}";
    }

}
