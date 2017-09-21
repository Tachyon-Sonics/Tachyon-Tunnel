package ch.tachyon.tunnel.demo.plugin.resample.impl;

import ch.tachyon.tunnel.demo.plugin.dsp.fft.*;

public class KaiserLiBuffer extends LinearInterpolatedBuffer {

    private final float scaleFactor;


    public KaiserLiBuffer(int nbSamplesPerZeroCross, int nbZeroCross, float kaiserAttenuationDb) {
        // We pad with (nbSamplesPerZeroCross / 2) + 2 additional zeroes (to avoid range checking)
        super(nbSamplesPerZeroCross * nbZeroCross + (nbSamplesPerZeroCross / 2) + 2);
        this.scaleFactor = (float) nbSamplesPerZeroCross / (float) Math.PI;
        float[] kaiserWindow = new float[nbSamplesPerZeroCross * nbZeroCross * 2];
        Windows.fillKaiserWindow(kaiserWindow, Windows.getKaiserBeta(kaiserAttenuationDb));
        for (int i = 0; i < super.getSize(); i++) {
            float windowCoeff = 0.0f;
            if (i <= kaiserWindow.length / 2)
                windowCoeff = kaiserWindow[kaiserWindow.length / 2 - i];
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
