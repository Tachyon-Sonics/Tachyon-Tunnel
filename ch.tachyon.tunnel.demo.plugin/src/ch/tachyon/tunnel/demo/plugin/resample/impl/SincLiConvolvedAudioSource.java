package ch.tachyon.tunnel.demo.plugin.resample.impl;

import java.util.*;

/**
 * Transform a source into a version that is convolved by a sinc function.
 * <p>
 * Resampling based on the <code>double</code> data type
 */
public class SincLiConvolvedAudioSource implements IAudioSource, IResetable {

    private final IAudioSource source;
    private final SincLiBuffer sincBuffer;
    private final int nbZeroCross;
    private final double step;
    private final float amplitudeCorrection;

    // Buffering of the source
    private final int bufferSize;
    private final int historySize;
    private final float[] buffer;
    private int eosIndex; // Number of valid samples in buffer
    private double index; // current index in buffer[]

    // Optimization consts
    private final static float fPI = (float) Math.PI;
    private final float startOffset;
    private final float deltaOffset;


    /**
     * @param source the audio source to upsample and convolve with a sinc
     * between every sample before convolving
     * @param nbSamplesPerZeroCross numbre of sample between zero crossing for the sinc function
     * @param nbZeroCross number of zero cross (on one side) for the sinc function
     * @param stretch the amount by which the sinc function must be stretched.
     * @param bufferSize the number of sample to read from the source at once
     * @param srcStep the rate at which the source must be read. 1=same as result, 0.5=half speed, etc
     * @param amplitudeCorrection amount by which output samples are multiplied
     */
    public SincLiConvolvedAudioSource(IAudioSource source, int nbSamplesPerZeroCross, int nbZeroCross,
            float stretch, int bufferSize, double srcStep, float amplitudeCorrection) {
        this.source = source;
        this.sincBuffer = new SincLiBuffer(nbSamplesPerZeroCross, nbZeroCross, stretch);
        this.nbZeroCross = nbZeroCross;
        this.bufferSize = bufferSize;
        this.historySize = nbZeroCross * 2 + 1;
        this.buffer = new float[bufferSize + historySize];
        this.amplitudeCorrection = amplitudeCorrection;
        this.step = srcStep;
        this.index = bufferSize + historySize - nbZeroCross - step;
        this.eosIndex = buffer.length - nbZeroCross;

        this.startOffset = (float) -nbZeroCross * fPI * sincBuffer.getScaleFactor();
        this.deltaOffset = fPI * sincBuffer.getScaleFactor();
    }

    public void reset() {
        Arrays.fill(buffer, 0.0f);
        this.index = bufferSize + historySize - nbZeroCross - step;
        this.eosIndex = buffer.length - nbZeroCross;
        if (source instanceof IResetable)
            ((IResetable) source).reset();
    }

    public int getSamples(float[] target, int start, int count) {
        int sourceIndex = 0;
        for (int i = 0; i < count; i++) {
            index += step;
            // Nearest index in buffer[]
            sourceIndex = (int) (index + 0.5);
            // Deviation from index (used as sample offset in the sinc function)
            float sincShift = (float) (index - sourceIndex) * deltaOffset;

            // Convolve source sample at sourceIndex
            float result = 0.0f;
            float sincOffset = startOffset + sincShift;
            sourceIndex -= prefetch(sourceIndex + nbZeroCross);
            for (int offset = -nbZeroCross; offset < 0; offset++) {
                float sample = buffer[sourceIndex - offset];
                float sinc = sincBuffer.getPreScaled(-sincOffset);
                result += sample * sinc;
                sincOffset += deltaOffset;
            }
            {
                float sample = buffer[sourceIndex];
                float sinc = sincBuffer.getPreScaled(sincOffset > 0.0f ? sincOffset : -sincOffset);
                result += sample * sinc;
                sincOffset += deltaOffset;
            }
            for (int offset = 1; offset <= nbZeroCross; offset++) {
                float sample = buffer[sourceIndex - offset];
                float sinc = sincBuffer.getPreScaled(sincOffset);
                result += sample * sinc;
                sincOffset += deltaOffset;
            }
            target[start + i] = result * amplitudeCorrection;
        }
        if (sourceIndex < eosIndex)
            return target.length;
        else {
            sourceIndex = (int) (index + step + 0.5); // Move to next sample (first unprocessed one)
            return Math.max((int) (target.length - (double) (sourceIndex - eosIndex) / step + 0.5), 0);
        }
    }

    private int prefetch(int maxIndex) {
        int result = 0;
        while (maxIndex >= buffer.length) {
            fetchNextBuffer();
            maxIndex -= bufferSize;
            result += bufferSize;
        }
        return result;
    }

    private void fetchNextBuffer() {
        // Copy history
        System.arraycopy(buffer, bufferSize, buffer, 0, historySize);
        // Get new samples
        eosIndex += source.getSamples(buffer, historySize, bufferSize);
        // Update sample index
        index -= bufferSize;
        eosIndex -= bufferSize;
    }

}
