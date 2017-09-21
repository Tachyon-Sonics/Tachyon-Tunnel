package ch.tachyon.tunnel.demo.plugin.resample.impl;

import java.util.*;

/**
 * Sweepable sinc resampling
 */
public class SweepableSincLiConvolvedAudioSource implements IAudioSource, IResetable {

    private final IAudioSource source;
    private final SincLiBuffer sincBuffer;
    private final KaiserLiBuffer kaiserBuffer;
    private final int nbZeroCross;
    private final float antiAliasing;
    private float stretch;
    private double step;
    private float amplitudeCorrection;

    // Buffering of the source
    private final int bufferSize;
    private final int historySize;
    private final float[] buffer;
    private int eosIndex; // Number of valid samples in buffer
    private double index;

    // Optimization consts
    private final static float fPI = (float) Math.PI;
    private final static float fPIhalf4check = fPI / 2.0f + 0.001f;


    /**
     * @param source the audio source to upsample and convolve with a sinc
     * between every sample before convolving
     * @param nbSamplesPerZeroCross numbre of sample between zero crossing for the sinc function
     * @param nbZeroCross number of zero cross (on one side) for the sinc function
     * @param kaiserAttenuationDb in dB, positive. Parameter for the kaiser window applied to the sinc function
     * @param bufferSize the number of sample to read from the source at once
     */
    public SweepableSincLiConvolvedAudioSource(IAudioSource source, int nbSamplesPerZeroCross, int nbZeroCross,
            float kaiserAttenuationDb, int bufferSize, float antiAliasing, double ratio) {
        this.source = source;
        this.sincBuffer = new SincLiBuffer(nbSamplesPerZeroCross, nbZeroCross);
        this.kaiserBuffer = new KaiserLiBuffer(nbSamplesPerZeroCross, nbZeroCross, kaiserAttenuationDb);
        this.nbZeroCross = nbZeroCross;
        this.antiAliasing = antiAliasing;
        this.bufferSize = bufferSize;
        this.historySize = nbZeroCross * 2 + 1;
        this.buffer = new float[bufferSize + historySize];
        this.index = bufferSize + historySize - nbZeroCross - 1.0 / ratio;
        this.eosIndex = buffer.length - nbZeroCross;
        setRatio(ratio);
    }

    public void setRatio(double ratio) {
        this.step = 1.0 / ratio;
        amplitudeCorrection = (float) Math.min(ratio, 1.0);
        if (ratio != 1.0 && antiAliasing != 0.0)
            amplitudeCorrection *= (1.0 - antiAliasing) + (antiAliasing * getBandWidth(nbZeroCross));
        // When upsampling, sinc is not stretched and lowpass filter is half the source sample rate
        // When downsampling, stretch the sinc so that lowpass filter is half the target sample rate
        stretch = 1.0f / amplitudeCorrection;
        assert stretch >= 1.0f;
    }

    static float getBandWidth(int nbZeroCross) {
        /*
         * Approx: Without window (or rect window), we have 1 / (nbZeroCross * 2 + 1)
         * We assume window widen main lobe width by 4 (quite accurate for 100dB rejection)
         * 1 is added instead of 0.25 to allow some aliasing when nbZeroCross is small
         */
        float rollOff = 1.0f / ((float) nbZeroCross / 2.0f + 1.0f);
        return 1.0f - rollOff;
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
            // Nearest index in source
            sourceIndex = (int) (index + 0.5);
            // Deviation from index (used as sample offset in the sinc function)
            float sincShift = (float) (index - sourceIndex) * fPI;
            assert sincShift >= -fPIhalf4check && sincShift <= fPIhalf4check;

            // Convolve source sample at sourceIndex
            float result = 0.0f;
            float sincOffset = (float) -nbZeroCross * fPI + sincShift;
            sourceIndex -= prefetch(sourceIndex + nbZeroCross);
            for (int offset = -nbZeroCross; offset <= nbZeroCross; offset++) {
                float sample = buffer[sourceIndex - offset];
                float sinc = sincBuffer.get(sincOffset / stretch);
                float kaiser = kaiserBuffer.get(sincOffset);
                result += sample * sinc * kaiser;
                sincOffset += fPI;
            }
            target[start + i] = result * amplitudeCorrection;
        }
        if (sourceIndex <= eosIndex)
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
        System.arraycopy(buffer, buffer.length - historySize, buffer, 0, historySize);
        // Get new samples
        eosIndex += source.getSamples(buffer, historySize, bufferSize);
        // Update sample index
        index -= bufferSize;
        eosIndex -= bufferSize;
    }

}
