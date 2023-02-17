package ch.tachyon.tritonus.test.util;

import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;

/**
 * Test audio source of arbitrary length, sample rate, channels, etc. Only supplies silence (0.0f sample values)
 */
public class TestAudioSource implements IMultiChanAudioSource {

    private final long length;
    private final int nbChans;
    private final float sampleRate;
    private final int nbBits;
    private final IProgressMonitor progress;
    private long offset;


    public TestAudioSource(long length, int nbChans, float sampleRate, int nbBits, IProgressMonitor progress) {
        this.length = length;
        this.nbChans = nbChans;
        this.sampleRate = sampleRate;
        this.nbBits = nbBits;
        this.progress = progress;
    }

    @Override
    public AudioStreamFormat getAudioFormat() {
        return new AudioStreamFormat(sampleRate, nbChans, nbBits);
    }

    @Override
    public int readSamples(float[][] target) {
        int requested = target[0].length;
        int result = requested;
        if (offset + requested > length)
            result = (int) (length - offset);

//        for (int chan = 0; chan < nbChans; chan++)
//            Arrays.fill(target[chan], 0, result, 0.0f);
        offset += result;
        if (progress != null)
            progress.setProgress(offset, length);
        return result;
    }

    @Override
    public void close() {

    }

}
