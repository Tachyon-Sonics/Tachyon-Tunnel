package ch.tachyon.tunnel.demo.plugin.timestretch.stft;

import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;

/**
 * Short-time Fourier transform, synthesis. Uses fixed, power-of-two block size and hop size.
 * <p>
 * For a single audio channel
 * @author Nicolas Juillerat
 *
 */
public class StftSynthesizer {

    static class SynthesisBuffer {

        public final float[] processBuffers;
        public final float[] olaBuffer;
        public int olaIndex;


        public SynthesisBuffer(int blockSize, int hopSize) {
            processBuffers = new float[blockSize];
            olaBuffer = new float[blockSize - hopSize];
            olaIndex = 0;
        }

    }
    
    
    private final int blockSize;
    private final int hopSize;
    private final float[] synthesisWindow;
    private float[] invWindow;
    private BooFFT fft;
    private SynthesisBuffer synthesisBuffer;


    public StftSynthesizer(int blockSize, int hopSize, float[] analysisWindow, float[] synthesisWindow) {
        this.blockSize = blockSize;
        this.hopSize = hopSize;
        if (synthesisWindow == null)
            synthesisWindow = analysisWindow;
        this.synthesisWindow = synthesisWindow;

        this.fft = new BooFFT(blockSize / 2);
        this.invWindow = new float[hopSize];
        boolean result = Windows.fillInverseWindow(analysisWindow, synthesisWindow, invWindow, blockSize, hopSize);
        if (!result)
           throw new IllegalStateException("Cannot create inverse window");
        synthesisBuffer = new SynthesisBuffer(blockSize, hopSize);
    }

    public void synthesize(Cmplx[][] spectrums, float[] output) {
        if (output.length != this.hopSize)
            throw new IllegalArgumentException("Invalid size. Must match hopSize * numHops " + hopSize);
        synthesize(synthesisBuffer, spectrums);
        renderResult(synthesisBuffer, output);
    }

    private void synthesize(SynthesisBuffer buffers, Cmplx[][] spectrums) {
        backwardFFT(spectrums[0], buffers.processBuffers);
        applySynthesisWindow(buffers.processBuffers);
    }

    private void backwardFFT(Cmplx[] spectrum, float[] output) {
        fft.backC2R(spectrum, output);
    }

    private void applySynthesisWindow(float[] output) {
        if (synthesisWindow != null) {
            for (int i = 0; i < output.length; i++)
                output[i] *= synthesisWindow[i];
        }
    }

    private void renderResult(SynthesisBuffer buffers, float[] output) {
        int olaSize = blockSize - hopSize;
        for (int i = 0; i < hopSize; i++)
            output[i] = (buffers.olaBuffer[(i + buffers.olaIndex) % olaSize] + buffers.processBuffers[i]) * invWindow[i];
        buffers.olaIndex = (buffers.olaIndex + hopSize) % olaSize;
        for (int i = 0; i < olaSize - hopSize; i++)
            buffers.olaBuffer[(i + buffers.olaIndex) % olaSize] += buffers.processBuffers[i + hopSize];
        for (int i = olaSize - hopSize; i < olaSize; i++)
            buffers.olaBuffer[(i + buffers.olaIndex) % olaSize] = buffers.processBuffers[i + hopSize];
    }

    /**
     * The block size (or FFT size) of this engine
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    public int getHopSize() {
        return this.hopSize;
    }

    /**
     * Output latency of a whole STFT based on this synthesizer and a matching stft analyzer
     */
    public int getLatency() {
        return this.blockSize - this.hopSize;
    }

    /**
     * Latency before the center point in an STFT frame gets synthesized.
     */
    public int getSynthesisLatency() {
        return this.blockSize / 2;
    }

}
