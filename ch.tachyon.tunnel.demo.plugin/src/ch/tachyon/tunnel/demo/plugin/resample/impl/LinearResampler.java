package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * {@link IResampler} implementation using linear interpolation. This implementation also implements
 * {@link ISweepableResampler}.
 */
public class LinearResampler extends BufferedResampler implements ISweepableResampler {

    private double step;
    private double index;


    /**
     * @param ratio output rate / input rate, or output length / input length.
     */
    public LinearResampler(IAudioSource source, double ratio, int bufferSize) {
        super(source, bufferSize, 1);
        // 1 output sample = 1/ratio input sample
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
        int floor = 0;
        for (int i = 0; i < output.length; i++) {
            index += step;
            floor = (int) index;

            // Get the two values around index
            floor -= super.prefetch(floor + 1);
            float v1 = super.getSample(floor);
            float v2 = super.getSample(floor + 1);

            // Linear interpolation
            float weight = (float) (index % 1.0);
            output[i] = v1 + (v2 - v1) * weight;
        }

        if (floor <= eosIndex)
            return output.length;
        else {
            floor = (int) (index + step);
            return (int) (output.length - (floor - eosIndex) / step + 0.5);
        }
    }

    @Override
    protected void fetchNextBuffer() {
        super.fetchNextBuffer();
        index -= bufferSize;
    }

    public int getLatency() {
        return 1;
    }

}
