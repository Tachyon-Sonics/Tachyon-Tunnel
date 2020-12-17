package ch.tachyon.tunnel.demo.plugin.timestretch;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;
import ch.tachyon.tunnel.demo.plugin.timestretch.stft.*;
import ch.tachyon.tunnel.demo.plugin.timestretch.stft.StftEngine.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * Base class for time stretching plugins based on the STFT. Implements all the required interfaces of the ch.tachyon.tunnel.plugin package, provides the
 * time stretch ratio, handles the short-time Fourier transform (STFT) using {@link StftEngine}, but leaves the actual frequency-domain processing abstract,
 * in {@link #process(Cmplx[], Cmplx[][])}.
 * @author Nicolas Juillerat
 *
 */
public abstract class AbstractTimeStretch implements IMultiChanPullEffect, IStartStop, IFixedChunkLength, ILatency, IRequiredBeforeAfter, IFourierProcessor {

    protected double ratio;
    protected StftEngine stftEngine;


    @Description("Time stretching ratio\nOutput length / input length\n< 1 to speed up\n> 1 to slow down")
    @Order(1)
    @Scale(ScaleType.LOGARITHMIC)
    @Range(minValue = 0.25, maxValue = 4.0, defaultValue = 1.0)
    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    protected abstract int getBlockSize();
    
    protected abstract int getOutputHopSize();
    
    protected abstract int getZeroPad();

    @Override
    public void startProcessing(IProcessingInfo info) {
        int zeroPad = getZeroPad();
        int blockSize = getBlockSize() / zeroPad;
        float[] window = new float[blockSize];
        Windows.fillHannWindow(window);
        if (zeroPad > 1) {
            float[] zeroPaddedWindow = new float[blockSize * zeroPad];
            System.arraycopy(window, 0, zeroPaddedWindow, (blockSize * zeroPad - blockSize) / 2, blockSize);
            window = zeroPaddedWindow;
        }
        stftEngine = new StftEngine(ratio, info.getNumberOfChannels(), getBlockSize(), getOutputHopSize(), window, window);
    }

    @Override
    public long getRequiredSamplesBefore(IProcessingInfo info) {
        return (long) (getLatency(IoDirection.INPUT) + 0.5);
    }

    @Override
    public long getRequiredSamplesAfter(IProcessingInfo info) {
        return 0;
    }

    protected double getOutputLatencyAsDouble() {
        return stftEngine.getLatency();
    }

    @Override
    public double getLatency(IoDirection ioDirection) {
        double outputLatency = getOutputLatencyAsDouble();
        if (ioDirection == IoDirection.INPUT)
            outputLatency = outputLatency / ratio;
        return outputLatency;
    }

    @Override
    public int getFixedChunkLength() {
        return stftEngine.getFixedChunkLength();
    }

    @Override
    public boolean canWriteFasterThanRead() {
        return ratio > 1.0;
    }

    @Override
    public int process(IMultiChanAudioSource source, float[][] output) {
        /*
         * This will
         * - do the forward short-time Fourier transform
         * - invoke the process(Cmplx[] source, Cmplx[][] spectrums) method below
         * - do the backward short-time Fourier transform
         */
        return stftEngine.process(source, this, output);
    }

    @Override
    public abstract void process(Cmplx[] source, Cmplx[][] spectrums);

    @Override
    public void stopProcessing() {
        stftEngine = null;
    }

}
