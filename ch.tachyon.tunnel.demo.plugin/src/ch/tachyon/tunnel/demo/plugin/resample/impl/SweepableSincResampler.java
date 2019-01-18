package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Sweepable sinc-based resampler based on <code>double</code> arithmetic
 */
public class SweepableSincResampler implements ISweepableResampler {

    private final SweepableSincLiConvolvedAudioSource source;
    private final int latency;


    /**
     * @param ratio output rate / input rate, or output length / input length.
     * 48000 to 24000 = 0.5; 24000 to 48000 = 2.0
     * @param antiAliasing a value between 0 and 1 that specifies by how much antialiasing should be
     * reduced at the expense of bandwidth. With a value of 0, full bandwidth is preserved and the amount of
     * aliasing is proportional to the transition width of the sinc low-pass filter. With a value of 1, the
     * bandwidth is reduced by an approximation of the transition width of the sinc low-pass filter, and
     * aliasing should be minimal. This argument is ignored if the resampling ration is 1.
     */
    public SweepableSincResampler(IAudioSource source, double ratio, int nbSamplesPerZeroCross, int nbZeroCross,
            float antiAliasing, int bufferSize) {
        if (nbZeroCross < 1)
            throw new IllegalArgumentException();
        this.source = new SweepableSincLiConvolvedAudioSource(source, nbSamplesPerZeroCross, nbZeroCross,
                bufferSize, antiAliasing, ratio);
        this.latency = nbZeroCross;
    }

    public void reset() {
        source.reset();
    }

    public int resample(float[] output) {
        return source.getSamples(output, 0, output.length);
    }

    public void setRatio(double ratio) {
        source.setRatio(ratio);
    }

    public int getLatency() {
        return latency;
    }

}
