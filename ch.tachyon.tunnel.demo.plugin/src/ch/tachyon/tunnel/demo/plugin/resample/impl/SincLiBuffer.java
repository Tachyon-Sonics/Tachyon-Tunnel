package ch.tachyon.tunnel.demo.plugin.resample.impl;

import ch.tachyon.tunnel.dsp.fft.*;

/**
 * {@link LinearInterpolatedBuffer} filled with the sinc function. The buffer
 * is accessed with interpolation using a floating index with {@link #get(float)}.
 * It can also be accessed without interpolation at integer indexes with {@link #get(int)}.
 */
public class SincLiBuffer extends LinearInterpolatedBuffer {

    /**
     * Multiplier that converts from an angle to an offset in the buffer
     */
    private final float scaleFactor;


    /**
     * Create a new buffer with a sinc function windowed by a Hann window
     */
    public SincLiBuffer(int nbSamplesPerZeroCross, int nbZeroCross, float stretch) {
        // We pad with (nbSamplesPerZeroCross / 2) + 2 additional zeroes (to avoid range checking)
        super(nbSamplesPerZeroCross * nbZeroCross + (nbSamplesPerZeroCross / 2) + 2);
        this.scaleFactor = (float) nbSamplesPerZeroCross / (float) Math.PI;
        fillData(nbSamplesPerZeroCross, nbZeroCross, stretch);
    }

    private void fillData(int samplesPerZeroCross, int nbZeroCross, float stretch) {
        float[] window = new float[samplesPerZeroCross * nbZeroCross * 2];
        Windows.fillHannWindow(window);
        // Create windowed sinc
        for (int i = 0; i < super.getSize(); i++) {
            float angle = (float) i / (float) samplesPerZeroCross;
            float windowCoeff = 0.0f;
            if (i <= window.length / 2)
                windowCoeff = window[window.length / 2 - i];
            float value = sinc(angle / stretch) * windowCoeff;
            super.set(i, value);
        }
    }

    /**
     * Create a new buffer with a sinc function, not windowed
     */
    SincLiBuffer(int nbSamplesPerZeroCross, int nbZeroCross) {
        super(nbSamplesPerZeroCross * nbZeroCross + (nbSamplesPerZeroCross / 2) + 2);
        this.scaleFactor = (float) nbSamplesPerZeroCross / (float) Math.PI;
        for (int i = 0; i < super.getSize(); i++) {
            float angle = (float) i / (float) nbSamplesPerZeroCross;
            super.set(i, sinc(angle));
        }
    }

    private float sinc(float angle) {
        if (angle == 0.0f)
            return 1.0f;
        return (float) (Math.sin(Math.PI * angle) / (Math.PI * angle));
    }

    @Override
    public float get(float angle) {
        float index = angle * scaleFactor;
        if (index < 0.0f)
            return super.get(-index);
        else
            return super.get(index);
    }

    public float getPreScaled(float angle) {
        return super.get(angle);
    }

    @Override
    public float get(int index) {
        if (index < 0)
            return super.get(-index);
        else
            return super.get(index);
    }

    public float getPos(int index) {
        // assert index >= 0
        return super.get(index);
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public static void main(String[] args) {
        SincLiBuffer buffer = new SincLiBuffer(10, 7, 1.0f);
        float max = (float) Math.PI * 7.5f;
        float step = (float) Math.PI / 20.0f;
        for (float a = -max; a < max; a+= step) {
            float value = buffer.get(a);
            int k = (int)((value + 1.0f) * 60.0f + 0.5f);
            for (int i = 30; i <= 120; i++) {
                if (i == k)
                    System.out.print("*");
                else if (i == 60)
                    System.out.print("|");
                else
                    System.out.print(" ");
            }
            System.out.println(a + ":" + value);
        }
    }

}
