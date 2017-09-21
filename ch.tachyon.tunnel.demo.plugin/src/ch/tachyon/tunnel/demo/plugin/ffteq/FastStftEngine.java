package ch.tachyon.tunnel.demo.plugin.ffteq;

import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Base class for audio processing using the Short-Time Fourier Transform. This engine is limited to
 * Hann windowing that sum up to a constant.
 * <p>
 * Typical usage:
 * <pre>
 * FastStftEngine engine = new FastStftEngine(4096, 4, true); // Use blocks of 4096 samples, with 4x overlapping and both analysis and synthesis windows
 * engine.init(); // Required
 * float[] input = new float[1024]; // Process by chunks whose size corresponds to the block size divided by the overlapping factor (4096 / 4 = 1024 in this example)
 * float[] output = new float[1024]; // The processed samples will go there. Note that you can use the same array for input and output if you want
 * while (...) {
 *    ... // fill the next 1024 samples into the input[] array
 *    Cmplx[] spectrum = engine.analyze(input); // perform the Short-Time Fourier Transform
 *    ... // Modify the spectrum as desired, in place. The spectrum size is blockSize / 2 + 1, 2049 in this example
 *    engine.synthesize(output); // Synthesize the result into the output[] array - using the previously returned spectrum array
 *    ... // Play or save the next 1024 samples from the output[] array
 *    // Note that the output is delayed compared to the input, as returned by getLatency()
 * }
 * </pre>
 * <p>
 * This class implements {@link IHasSerialSections}, which allows it to be used in a plugin that supports {@link MultiThreading} for faster processing
 * on multi-core systems. To benefit from it, a plugin using this class must implement {@link IHasSerialSections} and invoke the corresponding methods
 * of this class. Furthermore, {@link #init()} <i>must</i> be invoked during {@link IBeginProcessing#beginProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}
 * (which imply your plugin must implement that interface).
 * <p>
 * Note that the implementation is pretty basic and may not scale to more than a few cores unless the frequency domain processing has a
 * significant complexity (and takes most of the time).
 */
public class FastStftEngine implements IHasSerialSections {

    private final static String ANALYZER_SECTION_NAME = "FastStftEngine.analyzer";
    private final static String SYNTHESIZER_SECTION_NAME = "FastStftEngine.synthesizer";

    private final int numHops;
    private final int blockSize;
    private final int hopSize;
    private final int overlapFactor;
    private final int olaSize;
    private final int nbBins;
    private Buffer inBuffer;
    private float[][] inputBlocks;
    private BooFFT fft;
    private Cmplx[][] spectrums;
    private float correction;
    private final boolean synthesisWindow;
    private float[][] outBlocks;
    private Buffer olaBuffer;

    static class Buffer {
        public final float[] buffer;
        public int index;

        public Buffer(int size) {
            this.buffer = new float[size];
        }
    }

    private ISerialSection analyzerSection = new DummySerialSection();
    private ISerialSection synthesizerSection = new DummySerialSection();


    /**
     * Create a new engine for the given block size and overlap factor. The given engine
     * automatically applies a Hann analysis window to the input, and optionally applies
     * a Hann synthesis window before the overlap-add process.
     * @param blockSize the block size. In must be a power of two
     * @param overlapFactor the overlap factor. Must be a power of two &lt;=2 when no synthesis
     * window is used, and &lt;=4 when a synthesis window is used.
     * @param synthesisWindow whether to apply a synthesis window.
     */
    public FastStftEngine(int numHops, int blockSize, int overlapFactor, boolean synthesisWindow) {
        this.numHops = numHops;
        this.overlapFactor = overlapFactor;
        int minOverlap = (synthesisWindow ? 4 : 2);
        if (overlapFactor < minOverlap || ((overlapFactor & (overlapFactor - 1)) != 0))
            throw new IllegalArgumentException("overlapFactor must be a power of two >= " + minOverlap);
        this.blockSize = blockSize;
        this.hopSize = blockSize / overlapFactor;
        if ((hopSize & (hopSize - 1)) != 0)
            throw new IllegalArgumentException("blockSize / overlapFactor must be a power of 2");
        this.olaSize = blockSize - hopSize;
        this.nbBins = blockSize / 2 + 1;
        this.synthesisWindow = synthesisWindow;
    }

    public void init() {
        this.inputBlocks = new float[numHops][blockSize];
        this.fft = new BooFFT(blockSize / 2);
        this.spectrums = Cmplx.newArray(numHops, nbBins);
        this.correction = Windows.getHannCorrection(overlapFactor, synthesisWindow ? 2 : 1);
        this.outBlocks = new float[numHops][blockSize];
    }

    public Cmplx[][] analyze(float[] input) {
        if (input.length != this.hopSize * numHops)
            throw new IllegalArgumentException("Invalid size. Must match hopSize " + hopSize);
        analyzerSection.enterSerialSection();
        try {
            for (int hop = 0; hop < numHops; hop++) {
                System.arraycopy(input, hopSize * hop, inBuffer.buffer, inBuffer.index, hopSize);
                inBuffer.index = (inBuffer.index + hopSize) % blockSize;
                for (int i = 0; i < blockSize; i++)
                    inputBlocks[hop][i] = inBuffer.buffer[(i + inBuffer.index) % blockSize];
            }
        } finally {
            analyzerSection.leaveSerialSection();
        }
        for (int hop = 0; hop < numHops; hop++) {
            fft.forwR2C(inputBlocks[hop], spectrums[hop]);
            // Apply Hann window in the Fourier domain
            Windows.convolve2(spectrums[hop], Windows.HannCoefs[0], Windows.HannCoefs[1]);
        }
        return this.spectrums;
    }

    public void synthesize(float[] output) {
        if (output.length != this.hopSize * this.numHops)
            throw new IllegalArgumentException("Invalid size. Must match hopSize " + hopSize);
        for (int hop = 0; hop < numHops; hop++) {
            // Apply Hann synthesis window in the Fourier domain
            if (synthesisWindow)
                Windows.convolve2(spectrums[hop], Windows.HannCoefs[0], Windows.HannCoefs[1]);
            fft.backC2R(spectrums[hop], outBlocks[hop]);
        }
        // Overlap-add
        synthesizerSection.enterSerialSection();
        try {
            for (int hop = 0; hop < numHops; hop++) {
                for (int i = 0; i < hopSize; i++)
                    output[hopSize * hop + i] = (olaBuffer.buffer[(i + olaBuffer.index) % olaSize] + outBlocks[hop][i]) * correction;
                olaBuffer.index = (olaBuffer.index + hopSize) % olaSize;
                for (int i = 0; i < olaSize - hopSize; i++)
                    olaBuffer.buffer[(i + olaBuffer.index) % olaSize] += outBlocks[hop][i + hopSize];
                for (int i = olaSize - hopSize; i < olaSize; i++)
                    olaBuffer.buffer[(i + olaBuffer.index) % olaSize] = outBlocks[hop][i + hopSize];
            }
        } finally {
            synthesizerSection.leaveSerialSection();
        }
    }

    /**
     * The block size (or FFT size) of this engine
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    public int getInputSize() {
        return this.hopSize * this.numHops;
    }

    /**
     * The hop size (in samples) of this engine. Processing can only be done by chunks of the returned length
     */
    public int getHopSize() {
        return this.hopSize;
    }

    public int getNbBins() {
        return this.blockSize / 2 + 1;
    }

    public int getLatency() {
        return this.blockSize - this.hopSize;
    }

    // IHasSerialSection

    public void createSerialSections(ISerialSectionFactory factory) {
        this.inBuffer = new Buffer(blockSize);
        this.olaBuffer = new Buffer(blockSize);
        this.analyzerSection = factory.createSerialSection(ANALYZER_SECTION_NAME, inBuffer);
        this.synthesizerSection = factory.createSerialSection(SYNTHESIZER_SECTION_NAME, olaBuffer);
    }

    public void setSerialSections(ISerialSectionPool pool) {
        this.analyzerSection = pool.getSerialSection(ANALYZER_SECTION_NAME);
        this.synthesizerSection = pool.getSerialSection(SYNTHESIZER_SECTION_NAME);
        this.inBuffer = (Buffer) analyzerSection.getUserData();
        this.olaBuffer = (Buffer) synthesizerSection.getUserData();
    }

}
