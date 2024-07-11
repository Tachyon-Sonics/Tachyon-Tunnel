package ch.tachyon.tunnel.demo.plugin.resample.impl;

import ch.tachyon.tunnel.dsp.fft.*;

public class WindowLiBuffer extends LinearInterpolatedBuffer {

    private final float scaleFactor;


    public WindowLiBuffer(int nbSamplesPerZeroCross, int nbZeroCross) {
        // We pad with (nbSamplesPerZeroCross / 2) + 2 additional zeroes (to avoid range checking)
        super(nbSamplesPerZeroCross * nbZeroCross + (nbSamplesPerZeroCross / 2) + 2);
        this.scaleFactor = (float) nbSamplesPerZeroCross / (float) Math.PI;
        float[] window = new float[nbSamplesPerZeroCross * nbZeroCross * 2];
        Windows.fillHannWindow(window);
        for (int i = 0; i < super.getSize(); i++) {
            float windowCoeff = 0.0f;
            if (i <= window.length / 2)
                windowCoeff = window[window.length / 2 - i];
            super.set(i, windowCoeff);
        }
    }

    @Override
    public float get(float angle) {
        float index = angle * scaleFactor;
        if (index < 0.0f)
            return super.get(-index);
        else
            return super.get(index);
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

}
