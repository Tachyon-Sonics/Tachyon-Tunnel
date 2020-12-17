package ch.tachyon.tunnel.demo.plugin.timestretch;

import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.param.*;


/**
 * Audio Time stretching effect
 * @see GradientEngine for the actual audio time stretching logic
 * @author Nicolas Juillerat
 */
@Category("Tempo")
@Name("Time Stretching")
@Description("Change the speed without affecting the pitch\nBased on the \"gradient\" phase vocoder by Zdeněk Průša and Nicki Holighaus")
public class GradientTimeStretch extends AbstractTimeStretch implements IMultiChanPullEffect, IStartStop, IFixedChunkLength, ILatency, IRequiredBeforeAfter {

    private final static int BLOCK_SIZE = 4096; // At 44.1 kHz, automatically adjusted wrt actual sample rate
    private final static int OVERLAP = 4; // Without zero padding. With zero padding, BLOCK_SIZE and OVERLAP are doubled
    
    private double sampleRate = 44100.0;
    private boolean zeroPad = true;
    private GradientEngine timeStretchEngine;


    @Description("Enable zero padding.\nImproves quality at the cost of speed when ratio is significantly greater than 1")
    @Order(2)
    public boolean isZeroPad() {
        return zeroPad;
    }

    public void setZeroPad(boolean zeroPad) {
        this.zeroPad = zeroPad;
    }
    
    private int blockSize() { // Block size, without account for zero padding
        return adjustPowerOf2(BLOCK_SIZE, 44100.0f, sampleRate);
    }

    @Override
    protected int getBlockSize() { // Block size, including zero padding if enabled
        return blockSize() * getZeroPad();
    }

    @Override
    protected int getOutputHopSize() {
        int blockSize = blockSize() * getZeroPad();
        int overlap = OVERLAP * getZeroPad();
        return (blockSize / overlap);
    }

    @Override
    protected int getZeroPad() {
        return zeroPad ? 2 : 1;
    }
    
    @Override
    public void startProcessing(IProcessingInfo info) {
        this.sampleRate = info.getSampleRate();
        int blockSize = blockSize();
        int overlap = OVERLAP;
        int mult = zeroPad ? 2 : 1;
        double betaScale = 1.0;
        if (!zeroPad && ratio > 1)
            betaScale = 1.0 / ratio; // Empirical
        timeStretchEngine = new GradientEngine(ratio, blockSize * mult, overlap * mult, betaScale);
        timeStretchEngine.startProcessing(info);
        super.startProcessing(info);
    }

    private static int adjustPowerOf2(int value, double baseSampleRate, double actualSampleRate) {
        double sampleRateChange = actualSampleRate / baseSampleRate;
        double baseSR = 0.707;
        while (baseSR > sampleRateChange) {
            value = value / 2;
            baseSR = baseSR * 0.5;
        }
        while (baseSR * 2.0 < sampleRateChange) {
            value = value * 2;
            baseSR = baseSR * 2.0;
        }
        return value;
    }

    @Override
    protected double getOutputLatencyAsDouble() {
        double result = super.getOutputLatencyAsDouble();
        result += timeStretchEngine.getOutputLatency();
        return result;
    }

    /**
     * Spectrum processing: delegate to {@link GradientEngine}
     */
    @Override
    public void process(Cmplx[] source, Cmplx[][] spectrums) {
        int inHop = stftEngine.getCurrentInputHopSize();
        timeStretchEngine.process(inHop, source, spectrums);
    }

}
