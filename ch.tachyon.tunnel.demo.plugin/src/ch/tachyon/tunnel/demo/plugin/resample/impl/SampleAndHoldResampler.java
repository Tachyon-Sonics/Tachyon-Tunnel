package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Very simple, fast, and low quality {@link ISweepableResampler} implementation using
 * sample and hold.
 * <p>
 * This implementation is really only meant for completeness but is not of acceptable quality
 * in practice. It is recommended to use at least {@link LinearResampler} if high speed
 * resampling is desired. {@link LinearResampler} is twice as slow, but of much higher quality.
 */
public class SampleAndHoldResampler extends BufferedResampler implements ISweepableResampler {

    private double step;
    private double index;


    public SampleAndHoldResampler(IAudioSource source, double ratio, int bufferSize) {
        super(source, bufferSize, 0);
        this.step = 1.0 / ratio;
        reset();
    }

    public void setRatio(double ratio) {
        this.step = 1.0 / ratio;
    }

    @Override
    public void reset() {
        super.reset();
        this.index = bufferSize - step;
    }

    public int resample(float[] output) {
        int sourceIndex = 0;
        for (int i = 0; i < output.length; i++) {
            index += step;
            sourceIndex = (int) index;
            sourceIndex -= super.prefetch(sourceIndex);
            output[i] = super.getSample(sourceIndex);
        }
        if (sourceIndex <= eosIndex)
            return output.length;
        else {
            sourceIndex = (int) (index + step); // Move to first unprocessed sample
            return (int) (output.length - (sourceIndex - eosIndex) / step + 0.5);
        }
    }

    @Override
    protected void fetchNextBuffer() {
        super.fetchNextBuffer();
        index -= bufferSize;
    }

    public int getLatency() {
        return 0;
    }

}
