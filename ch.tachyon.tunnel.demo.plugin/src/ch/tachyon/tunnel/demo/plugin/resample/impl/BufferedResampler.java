package ch.tachyon.tunnel.demo.plugin.resample.impl;

import java.util.*;

/**
 * Base resampler implementation with a buffer and history
 */
public abstract class BufferedResampler implements IResampler {

    private final IAudioSource source;
    private final float[] buffer;
    protected final int bufferSize;
    private final int historySize;

    protected int eosIndex;


    public BufferedResampler(IAudioSource source, int bufferSize, int historySize) {
        this.source = source;
        this.buffer = new float[bufferSize + historySize];
        this.bufferSize = bufferSize;
        this.historySize = historySize;
        this.eosIndex = bufferSize;
    }

    public void reset() {
        Arrays.fill(buffer, 0.0f);
        this.eosIndex = bufferSize;
    }

    protected int prefetch(int sampleIndex) {
        int result = 0;
        while (sampleIndex >= buffer.length) {
            fetchNextBuffer();
            sampleIndex -= bufferSize;
            result += bufferSize;
        }
        return result;
    }

    protected final float getSample(int sampleIndex) {
        return buffer[sampleIndex];
    }

    protected void fetchNextBuffer() {
        // Copy history
        if (historySize > 0)
            System.arraycopy(buffer, buffer.length - historySize, buffer, 0, historySize);
        // Get new samples
        eosIndex += source.getSamples(buffer, historySize, bufferSize);
        eosIndex -= bufferSize;
    }

    protected IAudioSource getAudioSource() {
        return this.source;
    }

}
