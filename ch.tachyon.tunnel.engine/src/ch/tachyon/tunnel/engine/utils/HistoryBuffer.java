package ch.tachyon.tunnel.engine.utils;

public class HistoryBuffer {

    private final float[] data;
    private int index;


    public HistoryBuffer(int capacity) {
        this.data = new float[capacity];
    }

    public void write(float[] input) {
        for (int i = 0; i < input.length; i++) {
            data[index] = input[i];
            index = (index + 1) % data.length;
        }
    }

    public void write(float value) {
        data[index] = value;
        index = (index + 1) % data.length;
    }

    public void read(int pastOffset, float[] output) {
        read(pastOffset, output, output.length);
    }

    public void read(int pastOffset, float[] output, int amount) {
        if (pastOffset > -amount)
            throw new IllegalArgumentException();
        int offset = (index + data.length + pastOffset) % data.length;
        if (offset < 0)
            throw new IllegalArgumentException("Read past capacity");
        for (int i = 0; i < amount; i++)
            output[i] = data[(i + offset) % data.length];
    }

    public float read(int pastOffset) {
        int offset = (index + data.length + pastOffset) % data.length;
        if (offset < 0)
            throw new IllegalArgumentException("Read past capacity");
        return data[offset];
    }

}
