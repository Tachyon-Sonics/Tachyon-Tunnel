package ch.tachyon.tunnel.demo.plugin.timestretch.stft;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;

/**
 * Short-time Fourier transform for time stretching effects. Block size must be a power of two. Synthesis hop size must be a power of two
 * smaller than the block size. Analysis hop size is based on synthesis hop size and stretch ratio, and is hence not necessarily a power
 * of two, not even an integer.
 * <p>
 * Suitable for multiple audio channels (1 for mono, 2 for stereo, etc)
 * @author Nicolas Juillerat
 */
public class StftEngine {

    public static interface IFourierProcessor {

        /**
         * Transform an audio chunk in the frequency domain
         * @param source spectrum of the monophonic audio chunk (all channels summed)
         * @param spectrums spectrums of the audio chunk (first index is chan num, 2nd is fourier bin)
         */
        public void process(Cmplx[] source, Cmplx[][] spectrums);

    }

    private final double stretchRatio;
    private final int blockSize;
    private final int outputHopSize;
    private final int nbChans;
    private StftStretchAnalyzer analyzer;
    private Cmplx[][] spectrums; // chan x bin
    private Cmplx[] source;
    private StftSynthesizer[] synthesizers; // chan


    public StftEngine(double stretchRatio, int nbChans, int blockSize, int outputHopSize, float[] analysisWindow, float[] synthesisWindow) {
        this.stretchRatio = stretchRatio;
        this.nbChans = nbChans;
        this.blockSize = blockSize;
        this.outputHopSize = outputHopSize;
        double inputHopSize = (double) outputHopSize / stretchRatio;
        analyzer = new StftStretchAnalyzer(nbChans, blockSize, inputHopSize, analysisWindow);
        synthesizers = new StftSynthesizer[nbChans];
        for (int chan = 0; chan < nbChans; chan++) {
            synthesizers[chan] = new StftSynthesizer(blockSize, outputHopSize, analysisWindow, synthesisWindow);
        }
        source = Cmplx.newArray(getNbBins());
    }

    public int getFixedChunkLength() {
        return outputHopSize;
    }

    public int getNbBins() {
        return blockSize / 2 + 1;
    }

    public int getLatency() {
        int result = synthesizers[0].getLatency();
        result += (int) (synthesizers[0].getBlockSize() / 2.0f * (stretchRatio - 1.0f) + 0.5f); // Account for any time-scale
        return result;
    }

    /**
     * @return number of frames written into output. Must match {@link #getFixedChunkLength()} except for the last audio chunk.
     */
    public int process(IMultiChanAudioSource audioSource, IFourierProcessor processor, float[][] output) {
        int nbBins = getNbBins();

        // Analyze
        spectrums = analyzer.analyze(audioSource);

        // Create monophonic source
        for (int i = 0; i < nbBins; i++)
            source[i].set(spectrums[0][i]);
        for (int chan = 1; chan < nbChans; chan++) {
            for (int i = 0; i < nbBins; i++)
                source[i].add(spectrums[chan][i]);
        }

        // Process
        processor.process(source, spectrums);

        // Synthesize
        for (int chan = 0; chan < nbChans; chan++)
            synthesizers[chan].synthesize(new Cmplx[][] {spectrums[chan]}, output[chan]);

        // EOF handling
        int nbInputSamples = analyzer.getCurSamplesRead();
        int curHopSize = analyzer.getCurrentHopSize();
        if (nbInputSamples == curHopSize)
            return output[0].length;
        else
            return Math.min(output[0].length, (int) ((double) nbInputSamples * stretchRatio + 0.5));
    }

    public int getCurrentInputHopSize() {
        return analyzer.getCurrentHopSize();
    }

}
