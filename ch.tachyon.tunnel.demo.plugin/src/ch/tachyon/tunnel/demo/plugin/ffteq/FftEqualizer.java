package ch.tachyon.tunnel.demo.plugin.ffteq;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.dsp.*;
import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;


@Category("Filters")
@Name("FFT Equalizer")
@Description("Linear-Phase Frequency Equalizer\n10 bands + global gain\nImplemented using the Fast Fourier Transform (FFT)")
@SampleRates(min=44100, max=48000)
@Live
@MultiThreading
public class FftEqualizer implements ISimpleEffect, IStartStop, IBeginProcessing, IFixedChunkLength, ILatency, IHasSerialSections {

    private FastStftEngine engine = null;

    private float g32 = -4;
    private float g64 = -4;
    private float g125 = 8;
    private float g250 = 8;
    private float g500 = 0;
    private float g1000 = -4;
    private float g2000 = -8;
    private float g4000 = 0;
    private float g8000 = 8;
    private float g16000 = 4;
    private float globalGain = -4;
    
    private transient float sampleRate;
    private transient float[] equalizationCurve;


    @Order(1)
    @Name("32 Hz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG32() {
        return g32;
    }

    public void setG32(float g32) {
        this.g32 = g32;
    }

    @Order(2)
    @Name("64 Hz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG64() {
        return g64;
    }

    public void setG64(float g64) {
        this.g64 = g64;
    }

    @Order(3)
    @Name("125 Hz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG125() {
        return g125;
    }

    public void setG125(float g125) {
        this.g125 = g125;
    }

    @Order(4)
    @Name("250 Hz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG250() {
        return g250;
    }

    public void setG250(float g250) {
        this.g250 = g250;
    }

    @Order(5)
    @Name("500 Hz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG500() {
        return g500;
    }

    public void setG500(float g500) {
        this.g500 = g500;
    }

    @Order(6)
    @Name("1 KHz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG1000() {
        return g1000;
    }

    public void setG1000(float g1000) {
        this.g1000 = g1000;
    }

    @Order(7)
    @Name("2 kHz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG2000() {
        return g2000;
    }

    public void setG2000(float g2000) {
        this.g2000 = g2000;
    }

    @Order(8)
    @Name("4 kHz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG4000() {
        return g4000;
    }

    public void setG4000(float g4000) {
        this.g4000 = g4000;
    }

    @Order(9)
    @Name("8 kHz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG8000() {
        return g8000;
    }

    public void setG8000(float g8000) {
        this.g8000 = g8000;
    }

    @Order(10)
    @Name("16 kHz")
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getG16000() {
        return g16000;
    }

    public void setG16000(float g16000) {
        this.g16000 = g16000;
    }

    @Order(11)
    @Unit("dB")
    @Range(minValue=-20, maxValue=20)
    @Scale(value=ScaleType.LINEAR, steps=20)
    public float getGlobalGain() {
        return globalGain;
    }

    public void setGlobalGain(float globalGain) {
        this.globalGain = globalGain;
    }


    // Processing

    public void startProcessing(IProcessingInfo info) {
        this.engine = new FastStftEngine(1, 2048, 4, true);
        this.sampleRate = info.getSampleRate();
        
        /*
         * When processing using n cores, startProcessing() is invoked n times (each time on a different instance).
         * However, we do not need to compute the same curve n times (although it
         * would not harm). Here we check if it has already been computed and use it
         * then. Else we compute it and store it to processing storage so it is
         * available to other cores
         */
        equalizationCurve = info.getStorage().getProcessData(float[].class);
        if (equalizationCurve == null) {
            equalizationCurve = computeCurve(2048 / 2 + 1);
            info.getStorage().setProcessData(equalizationCurve);
        }
    }

    private float[] computeCurve(int nbBins) {
        float[] curve = new float[nbBins];
        final float[] Freqs = new float[] {32.0f, 64.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
        final float[] Amps = new float[] {getG32(), getG64(), getG125(), getG250(), getG500(), getG1000(), getG2000(), getG4000(), getG8000(), getG16000()};
        assert Freqs.length == Amps.length;
        final int NbBands = Freqs.length;
        int b0 = -1;
        int b1 = 0;
        float nextBin = freq2bin(nbBins, Freqs[0]);
        for (int i = 0; i < nbBins; i++) {
            while (i >= nextBin) {
                b0++;
                b1++;
                if (b1 < NbBands)
                    nextBin = freq2bin(nbBins, Freqs[b1]);
                else
                    nextBin = nbBins;
            }
            if (b0 < 0) {
                // DC to first band
                curve[i] = (float) AudioMath.dbToLevel(Amps[b1]);
            } else if (b1 >= Freqs.length) {
                // Last band to Nyquist
                curve[i] = (float) AudioMath.dbToLevel(Amps[b0]);
            } else {
                // Interpolation
                // Value before and after current pos
                assert b0 >= 0;
                float a1 = Amps[b0];
                float a2 = (b1 >= NbBands ? Amps[NbBands - 1] : Amps[b1]);
                // Interpolation position, based on log frequency
                double f0Log = Math.log(Freqs[b0]);
                double f1Log = Math.log(Freqs[b1]);
                double freq = bin2freq(nbBins, i);
                double freqLog = Math.log(freq);
                double mu = (freqLog - f0Log) / (f1Log - f0Log);
                assert mu >= 0.0 && mu <= 1.0 : mu;
                // Apply Hann curve instead of plain linear interpolation
                mu = Math.sin(mu * Math.PI / 2.0); // Hann
                double result = a1 * (1.0 - mu) + a2 * mu; // Interpolate (linear)
                curve[i] = (float) AudioMath.dbToLevel(result);
            }
        }
        return curve;
    }

    private float freq2bin(int nbBins, float freq) {
        int fftSize = (nbBins - 1) * 2;
        return (freq * (float) fftSize / sampleRate);
    }

    private float bin2freq(int nbBins, int binNum) {
        int fftSize = (nbBins - 1) * 2;
        return (float) binNum * sampleRate / (float) fftSize;
    }

    /**
     * Implemented to ensure we only receive audio chunks of the right size
     */
    public int getFixedChunkLength() {
        return engine.getInputSize();
    }

    /**
     * Implemented to report some latency to the tachyon tunnel engine so that it can compensate it
     */
    public double getLatency(IoDirection ioDirection) {
        if (engine == null)
            return 0;
        return engine.getLatency();
    }

    /**
     * When using n cores, this method is invoked n times, each time on a different instance.
     * <p>
     * Unlike {@link #startProcessing(IProcessingInfo)}, the n calls are performed concurrently.
     */
    public void beginProcessing(IProcessingInfo info) {
        engine.init();
    }

    public void process(float[] data) {
        float gain = (float) AudioMath.dbToLevel(globalGain);
        Cmplx[][] spectrums = engine.analyze(data);
        for (Cmplx[] spectrum : spectrums) {
            for (int i = 0; i < spectrum.length; i++)
                spectrum[i].mul(equalizationCurve[i] * gain);
        }
        engine.synthesize(data);
    }

    public void stopProcessing() {
        this.engine = null;
    }

    // IHasSerialSection

    public void createSerialSections(ISerialSectionFactory factory) {
        engine.createSerialSections(factory);
    }

    public void setSerialSections(ISerialSectionPool pool) {
        engine.setSerialSections(pool);
    }

}
