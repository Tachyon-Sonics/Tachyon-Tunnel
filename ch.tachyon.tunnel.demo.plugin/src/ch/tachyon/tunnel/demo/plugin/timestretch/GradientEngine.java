package ch.tachyon.tunnel.demo.plugin.timestretch;

import java.util.*;

import ch.tachyon.tunnel.demo.plugin.dsp.*;
import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;
import ch.tachyon.tunnel.plugin.*;

/**
 * Implementation of time-stretching in the frequency domain (assuming a short-time Fourier transform)
 * <p>
 * Based on the paper "Phase Vocoder Done Right" by Zdeněk Průša and Nicki Holighaus, 25<sup>th</sup> European Signal Processing Conference (EUSIPCO), 2017
 * <p>
 * With the following changes:
 * <ul>
 * <li>Frequency estimates are based on current and previous frame only (next (future) frame is not used)
 * <li>All bins are processed (the tolerence factor, that leaves random phases to almost silent bins, is not used)
 * <li>Zero padding is optional (it does not seem to improve the quality when ratio &lt; 1 (speeding up))
 * <li>Vertical phase propagation (when used) is based on Laroche &amp; Dolson's phase-locked vocoder ([5] of paper above), and hence has a 'beta' parameter
 * <ul>
 * <li>Using beta == ratio and enabling zero padding seems to mostly correspond to the original algorithm in the paper "Phase Vocoder Done Right"
 * <li>For ratio &gt; 1 (slowing down), and when zero padding is not used, beta is progressively moved to 1 (as ratio increases) to compensate for
 * transient duplications, at the expense of a more metallic sound.
 * </ul>
 * </ul>
 */
public class GradientEngine {

    private final double ratio;
    private final int blockSize;
    private final int overlap;
    private final int nbBins;
    private final double betaScale;

    private double inHop;
    private double outHop;

    private float[] powerMags; // nbBins * 2: previous, current
    private FloatIndexBinaryMaxHeap heap;
    private BitSet processedIndexes;

    private float[] inputPhases;
    private float[] prevInputPhases;
    private Cmplx[] prevOutput;
    private double[] outputPhases;
    private double[] phaseRotations;
    private Cmplx[] rotators;


    public GradientEngine(double ratio, int blockSize, int overlap, double betaScale) {
        this.ratio = ratio;
        this.blockSize = blockSize;
        this.overlap = overlap;
        this.nbBins = blockSize / 2 + 1;
        this.betaScale = betaScale;
    }

    public void startProcessing(IProcessingInfo info) {
        inputPhases = new float[nbBins];
        prevInputPhases = new float[nbBins];
        prevOutput = Cmplx.newArray(nbBins);
        outputPhases = new double[nbBins];
        phaseRotations = new double[nbBins];
        rotators = Cmplx.newArray(nbBins);
        for (int i = 0; i < nbBins; i++)
            rotators[i].set(1.0f, 0.0f);
        Random rnd = new Random(0);
        for (int i = 1; i < nbBins - 1; i++) {
            prevInputPhases[i] = (float) ((rnd.nextDouble() - 0.5) * Math.PI * 2.0);
            prevOutput[i].set((float) Math.sin(prevInputPhases[i]), (float) Math.cos(prevInputPhases[i]));
        }

        powerMags = new float[nbBins * 2];
        heap = new FloatIndexBinaryMaxHeap(powerMags);
        processedIndexes = new BitSet(nbBins);
    }

    public void process(int inHop, Cmplx[] source, Cmplx[][] spectrums) {
        normalize(source, spectrums);

        this.inHop = inHop;
        outHop = (double) blockSize / overlap;

        for (int i = 0; i < nbBins; i++)
            inputPhases[i] = source[i].phiApprox();

        // Shift previous magnitudes
        System.arraycopy(powerMags, nbBins, powerMags, 0, nbBins);

        // Compute new magnitudes
        for (int i = 0; i < nbBins; i++)
            powerMags[nbBins + i] = source[i].powerMag();

        // Put previous magnitudes in the heap
        assert heap.isEmpty();
        processedIndexes.clear();
        for (int i = 0; i < nbBins; i++)
            heap.add(i); // Previous frame, bin 'i'

        double beta = (1.0 - betaScale) + betaScale * ratio;

        // Iterate
        while (!heap.isEmpty()) {
            int maxIndex = heap.poll();
            if (maxIndex < nbBins) {
                // Interpolate in time from previous frame - standard vocoder
                int binNum = maxIndex;
                if (!processedIndexes.get(binNum)) {
                    calculateOutputPhases(source, binNum); // Time interpolation
                    phaseRotations[binNum] = outputPhases[binNum] - inputPhases[binNum];

                    // Add to heap
                    heap.add(nbBins + binNum); // Current frame, bin 'binNum'
                    processedIndexes.set(binNum);
                }
            } else {
                // Interpolate in frequency to neighbours of current frame
                int binNum = maxIndex - nbBins;
                if (binNum > 0) {
                    // Previous bin
                    int prevBin = binNum - 1;
                    if (!processedIndexes.get(prevBin)) {
                        double curPhase = inputPhases[binNum];
                        double prevPhase = inputPhases[prevBin];
                        double phaseDiff = prevPhase - curPhase;
                        if (phaseDiff > Math.PI)
                            phaseDiff -= 2.0 * Math.PI;
                        else if (phaseDiff < -Math.PI)
                            phaseDiff += 2.0 * Math.PI;

                        double updatedPhase = outputPhases[binNum] + beta * phaseDiff;
                        outputPhases[prevBin] = updatedPhase;

                        phaseRotations[prevBin] = updatedPhase - inputPhases[prevBin]; // θ

                        // Add to heap
                        heap.add(nbBins + prevBin); // Current frame, bin 'prevBin'
                        processedIndexes.set(prevBin);
                    }
                }
                if (binNum < nbBins - 1) {
                    // Next bin
                    int nextBin = binNum + 1;
                    if (!processedIndexes.get(nextBin)) {
                        double curPhase = inputPhases[binNum];
                        double nextPhase = inputPhases[nextBin];
                        double phaseDiff = nextPhase - curPhase;
                        if (phaseDiff > Math.PI)
                            phaseDiff -= 2.0 * Math.PI;
                        else if (phaseDiff < -Math.PI)
                            phaseDiff += 2.0 * Math.PI;

                        double updatedPhase = outputPhases[binNum] + beta * phaseDiff;
                        outputPhases[nextBin] = updatedPhase;

                        phaseRotations[nextBin] = updatedPhase - inputPhases[nextBin]; // θ

                        // Add to heap
                        heap.add(nbBins + nextBin); // Current frame, bin 'nextBin'
                        processedIndexes.set(nextBin);
                    }
                }
            }
        }

        assert processedIndexes.cardinality() == nbBins;

        createRotators();
        
        applyPhasePropagationOnBackup(source);
        applyPhasePropagationOnSignal(spectrums);

        // Backup source
        for (int i = 0; i < nbBins; i++)
            prevInputPhases[i] = inputPhases[i];

        normalize(source, spectrums);
    }

    private void normalize(Cmplx[] source, Cmplx[][] spectrums) {
        // Necessary because our real-FFT returns the wrong half of the complex spectrum
        for (int i = 0; i < nbBins; i++) {
            source[i].im *= -1.0f;
        }
        for (int chan = 0; chan < spectrums.length; chan++) {
            for (int i = 0; i < nbBins; i++) {
                spectrums[chan][i].im *= -1.0f;
            }
        }

        // Necessary because our FFT uses arrays indexed from 0 to N-1, but this algorithm assumes arrays indexed from -N/2 to N/2-1 (N = blockSize)
        for (int i = 0; i < nbBins; i += 2) {
            source[i].mul(-1.0f);
        }
        for (int chan = 0; chan < spectrums.length; chan++) {
            for (int i = 0; i < nbBins; i += 2) {
                spectrums[chan][i].mul(-1.0f);
            }
        }
    }

    private void calculateOutputPhases(Cmplx[] source, int binNum) {
        // Calculate phase shift
        float phase = inputPhases[binNum];
        float prevPhase = prevInputPhases[binNum];
        float phaseShift = phase - prevPhase;
        if (phaseShift > Math.PI)
            phaseShift -= Math.PI * 2.0;
        else if (phaseShift < -Math.PI)
            phaseShift += Math.PI * 2.0;

        double frequency = calculateFrequency(binNum, phaseShift);
        // [(7)] as = outHop, frequency = Δt Φa
        outputPhases[binNum] = prevOutput[binNum].phiApprox() + frequency * outHop; // (6), (15)
    }

    private double calculateFrequency(int binNum, float phaseShift) {
        double binFreq = (Math.PI * 2.0 * (double) binNum / (double) blockSize); // Ωk
        // Real bin center frequency in Hz is given by : binFreq * sampleRate / (Math.PI * 2.0)

        double inPhaseShift = phaseShift;
        double expectedShift = inHop * binFreq; // RaΩk [2πmaa/M]
        double phi = FastMath.wrap(inPhaseShift - expectedShift); // ΔΦ (Heterodyned phase increment) [(14) * aa], aa = inHop
        double frequency = binFreq + phi / inHop; // ω (5) "phi / inHop" = [(14)]
        // Real frequency in Hz is given by : frequency * sampleRate / (Math.PI * 2.0)
        return frequency;
    }

    private void createRotators() {
        for (int i = 0; i < nbBins; i++) {
            rotators[i].set(1.0f, (float) phaseRotations[i]);
            rotators[i].toCartesianApprox();
        }
    }

    private void applyPhasePropagationOnSignal(Cmplx[][] spectrums) {
        int nbChans = spectrums.length;
        for (int chan = 0; chan < nbChans; chan++) {
            Cmplx[] spectrum = spectrums[chan];
            for (int i = 0; i < nbBins; i++)
                spectrum[i].mul(rotators[i]);
        }
    }

    private void applyPhasePropagationOnBackup(Cmplx[] source) {
        for (int i = 0; i < nbBins; i++) {
            prevOutput[i].set(source[i]);
            prevOutput[i].mul(rotators[i]);
        }
    }

}
