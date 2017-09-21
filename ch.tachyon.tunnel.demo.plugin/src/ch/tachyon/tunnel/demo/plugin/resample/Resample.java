package ch.tachyon.tunnel.demo.plugin.resample;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.resample.impl.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.param.*;


@Category("Tempo")
@Name("Resample")
@Description("Change the speed, affecting both the pitch and tempo")
public class Resample implements IPullEffect, IStartStop, ILatency, IAudioSource {

    public final static ParameterDefinition<Resample, Double> RATIO = new ParameterDefinition<Resample, Double>(Resample.class, Double.class, "ratio");
    public final static ParameterDefinition<Resample, Integer> QUALITY = new ParameterDefinition<Resample, Integer>(Resample.class, Integer.class, "accuracy");
    public final static ParameterDefinition<Resample, Boolean> LOW_LATENCY = new ParameterDefinition<Resample, Boolean>(Resample.class, Boolean.class, "lowLatency");

    private final static int BUFFER_SIZE_STD = 8192;
    private final static int BUFFER_SIZE_LOW_LATENCY = 128;


    private double ratio;
    private int accuracy = 3;
    private boolean lowLatency = false;

    private IResampler resampler;
    private float[] sourceBuffer;
    private ISingleChanAudioSource hostSource;


    @Order(1)
    @Scale(ScaleType.LOGARITHMIC)
    @Range(minValue=0.1, maxValue=10.0, defaultValue=1.0)
    @Name("Ratio")
    @Description("Ratio between new speed and old speed\n" +
                "Values greater than one increase the tempo and the pitch\n" +
                "Values less than one decrease the tempo and the pitch")
    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    @Order(2)
    @Range(minValue=0, maxValue=5, defaultValue=3)
    @Name("Quality")
    @Description("Precision of the result\n" +
    		"Higher values mean better quality but slower processing")
    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int quality) {
        this.accuracy = quality;
    }

    @Order(3)
    @Name("Low Latency")
    @Range(defaultValue=0)
    @LatencyOnly(speedOptimalValue=0.0, latencyOptimalValue=1.0)
    @Description("Whether to use a low latency (but slower) version\n" +
    		"The quality is not affected by this parameter")
    public boolean isLowLatency() {
        return lowLatency;
    }

    public void setLowLatency(boolean lowLatency) {
        this.lowLatency = lowLatency;
    }

    // Source

    /**
     * {@link IAudioSource} implementation that is suitable for {@link IResampler}.
     * The implementation uses the {@link ISingleChanAudioSource} supplied by the host.
     * <p>
     * Because the {@link IAudioSource} and {@link ISingleChanAudioSource} interface slightly
     * differ, this method must do the appropriate conversion.
     */
    public int getSamples(float[] target, int offset, int count) {
        int amount = hostSource.readSamples(sourceBuffer);
        System.arraycopy(sourceBuffer, 0, target, offset, amount);
        if (amount < count)
            Arrays.fill(target, offset + amount, offset + count, 0.0f);
        return amount;
    }

    // Processing

    /**
     * Overriden to initialize the {@link IResampler} and prepare the processing buffers
     */
    public void startProcessing(IProcessingInfo info) {
        int bufferSize = (lowLatency ? BUFFER_SIZE_LOW_LATENCY : BUFFER_SIZE_STD);
        resampler = ResamplerFactory.getResampler(this, 1.0 / ratio, accuracy, false, bufferSize);
        sourceBuffer = new float[bufferSize];
    }

    public boolean canWriteFasterThanRead() {
        return ratio < 1.0;
    }

    public int getLatency(IoDirection ioDirection) {
        if (ioDirection == IoDirection.INPUT)
            return resampler.getLatency();
        else
            return (int) (resampler.getLatency() / ratio + 0.5); // !! Resampler ratio is 1.0 / this.ratio
    }

    public int process(ISingleChanAudioSource source, float[] output) {
        hostSource = source;
        // The resampler will use this as a source, namely #getSamples()
        return resampler.resample(output);
    }

    public void stopProcessing() {
        resampler = null;
        sourceBuffer = null;
        hostSource = null;
    }

}
