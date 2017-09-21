package ch.tachyon.tunnel.engine.utils;

import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Concurrent version of {@link HistoryBuffer}.
 * <p>
 * Be aware of the following differences with {@link HistoryBuffer}:
 * <ul>
 * <li>When reading after writing, there is no need to substract the size of the written data to the read offset, as the
 * offset is entirely determined by the clock.
 * <li>When reading and writing buffers, the clock must probably be multiplied by the buffers' length
 * </ul>
 */
public class ConcurrentHistoryBuffer {

    private final int size;
    private final float[] data;


    /**
     * @param size the size of the history buffer. Determines how much is the past we can go
     * @param capacity generally <code>size + blockSize * {@link IMtContext#getSerialRunningMaxSkew()}</code>
     */
    public ConcurrentHistoryBuffer(int size, int capacity) {
        this.size = size;
        this.data = new float[capacity];
    }

    public void write(float[] input, long clock) {
        write(input, input.length, clock);
    }

    public void write(float[] input, int length, long clock) {
        int index = (int) (clock % data.length);
        for (int i = 0; i < length; i++) {
            data[index] = input[i];
            index = (index + 1) % data.length;
        }
    }

    public void write(float value, long clock) {
        int index = (int) (clock % data.length);
        data[index] = value;
    }

    public void read(int pastOffset, float[] output, long clock) {
        read(pastOffset, output, output.length, clock);
    }

    public void read(int pastOffset, float[] output, int amount, long clock) {
        if (pastOffset < -size)
            throw new IllegalArgumentException();
        int index = (int) (clock % data.length);
        int offset = (index + data.length + pastOffset) % data.length;
        if (offset < 0)
            throw new IllegalArgumentException("Read past capacity");
        for (int i = 0; i < amount; i++)
            output[i] = data[(i + offset) % data.length];
    }

    public float read(int pastOffset, long clock) {
        int index = (int) (clock % data.length);
        int offset = (index + data.length + pastOffset) % data.length;
        if (offset < 0)
            throw new IllegalArgumentException("Read past capacity");
        return data[offset];
    }

}
