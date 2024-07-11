package ch.tachyon.tunnel.demo.plugin.timestretch.stft;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.dsp.fft.*;

/**
 * Short-time Fourier transform, analysis part.
 * <p>
 * Accepts a arbitrary non-integer and non power-of-two hop size. The actual integer hop size will vary from frame to frame around the exact value
 * (using dynamic error correction) and is available by {@link #getCurrentHopSize()}. Block size must be a power of two.
 * <p>
 * Suitable for multiple audio channels
 * @author Nicolas Juillerat
 */
public class StftStretchAnalyzer {

    static class AnalysisBuffer {

        public final float[] buffer;
        public int index;


        public AnalysisBuffer(int size) {
            this.buffer = new float[size];
        }

    }

    private final int nbChans;
    private final int blockSize;
    private final double hopSize;
    private final AnalysisBuffer[] inBuffers;
    private final float[] analysisWindow;
    private final BooFFT fft;
    private final Cmplx[][] spectrums;
    private final float[] processBuffer;

    private double curSkew;
    private int curHopSize;
    private int curSamplesRead;


    /**
     * @param hopSize input hopSize (with account for stretch ratio)
     */
    public StftStretchAnalyzer(int nbChans, int blockSize, double hopSize, float[] analysisWindow) {
        this.nbChans = nbChans;
        this.blockSize = blockSize;
        this.hopSize = hopSize;
        int nbBins = blockSize / 2 + 1;
        this.analysisWindow = analysisWindow;
        this.inBuffers = new AnalysisBuffer[nbChans];
        for (int chan = 0; chan < nbChans; chan++)
            this.inBuffers[chan] = new AnalysisBuffer(blockSize);
        this.fft = new BooFFT(blockSize / 2);
        this.spectrums = Cmplx.newArray(nbChans, nbBins);
        this.processBuffer = new float[blockSize];
        this.curSkew = 0.0;
    }

    public Cmplx[][] analyze(IMultiChanAudioSource source) {
        // Retrieve next hop
        curHopSize = (int) (hopSize - curSkew + 0.5);
        curSkew += ((double) curHopSize - hopSize);
        float[][] hopBuffers = new float[nbChans][curHopSize];
        curSamplesRead = source.readSamples(hopBuffers);

        // Copy to circular buffer
        for (int chan = 0; chan < nbChans; chan++) {
            AnalysisBuffer inBuffer = inBuffers[chan];
            float[] hopBuffer = hopBuffers[chan];

            for (int i = 0; i < curHopSize; i++)
                inBuffer.buffer[(i + inBuffer.index) % blockSize] = hopBuffer[i];

            inBuffer.index = (inBuffer.index + curHopSize) % blockSize;
        }

        for (int chan = 0; chan < nbChans; chan++) {
            AnalysisBuffer inBuffer = inBuffers[chan];
            
            // Create next block & apply analysis window
            for (int i = 0; i < blockSize; i++)
                processBuffer[i] = inBuffer.buffer[(i + inBuffer.index) % blockSize] * analysisWindow[i];

            // Forward FFT
            fft.forwR2C(processBuffer, spectrums[chan]);
        }
        return this.spectrums;
    }

    public int getNbChans() {
        return this.nbChans;
    }

    /**
     * The block size (or FFT size) of this engine
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    public double getHopSize() {
        return this.hopSize;
    }

    public int getCurrentHopSize() {
        return this.curHopSize;
    }

    public int getCurSamplesRead() {
        return curSamplesRead;
    }

    public int getNbBins() {
        return spectrums[0].length;
    }

}
