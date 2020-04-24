package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * High quality sinc-based resampling based on <code>double</code> arithmetic.
 */
public class SincResampler implements IResampler {

    private final SincLiConvolvedAudioSource source;
    private final int latency;


    /**
     * @param ratio output rate / input rate, or output length / input length.
     * 48000 to 24000 = 0.5; 24000 to 48000 = 2.0
     * @param antiAliasing a value between 0 and 1 that specifies by how much antialiasing should be
     * reduced at the expense of bandwidth. With a value of 0, full bandwidth is preserved and the amount of
     * aliasing is proportional to the transition width of the sinc low-pass filter. With a value of 1, the
     * bandwidth is reduced by an approximation of the transition width of the sinc low-pass filter, and
     * aliasing should be minimal. This argument is ignored if the resampling ratio is 1.
     * @param bufferSize the number of samples to read from the source at once.
     * {@link IAudioSource#getSamples(float[], int, int)} will always be invoked with this value as the
     * 'count' parameter.
     */
    public SincResampler(IAudioSource source, double ratio, int nbSamplesPerZeroCross, int nbZeroCross,
            float antiAliasing, int bufferSize) {
        if (nbZeroCross < 1)
            throw new IllegalArgumentException();
        /*
         * Down-sampling will stretch the sinc function, reducing the effective number of zero cross.
         * If the number of zero corss is too low, amplitude issues occur. Here we ensure
         * we have at least 3 zero cross
         */
        while (ratio * nbZeroCross < 3.0)
            nbZeroCross++;
        float amplitudeCorrection = (float) Math.min(ratio, 1.0);
        // When upsampling, sinc is not stretched and lowpass filter is half the source sample rate
        // When downsampling, stretch the sinc so that lowpass filter is half the target sample rate
        float sincStretch = 1.0f / amplitudeCorrection;
        double srcStep = 1.0 / ratio;
        this.source = new SincLiConvolvedAudioSource(source, nbSamplesPerZeroCross, nbZeroCross,
                sincStretch, bufferSize, srcStep, amplitudeCorrection);
        this.latency = nbZeroCross;
    }

    public void reset() {
        source.reset();
    }

    public int resample(float[] output) {
        return source.getSamples(output, 0, output.length);
    }

    public int getLatency() {
        return latency;
    }

}
