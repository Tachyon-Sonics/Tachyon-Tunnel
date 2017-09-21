package ch.tachyon.tunnel.host;

import java.util.*;

import ch.tachyon.tunnel.common.*;

/**
 * {@link IMultiChanAudioSink} implementation that collects the audio frames in memory. The collected
 * frames can be retrieved at any time as a <tt>float[][]</tt> array using {@link #toArray()}.
 */
public class MultiChanArraySink implements IMultiChanAudioSink {
    
    private final List<float[][]> data = new ArrayList<float[][]>();
    private int nbChannels;
    private int length;

    
    public void writeSamples(float[][] samples) {
        if (nbChannels == 0)
            nbChannels = samples.length;
        else if (nbChannels != samples.length)
            throw new IllegalArgumentException("The written samples do not have the same number of channels");
        int nbFrames = 0;
        for (int chan = 0; chan < nbChannels; chan++) {
            if (chan == 0)
                nbFrames = samples[0].length;
            else if (nbFrames!= samples[chan].length)
                throw new IllegalArgumentException("The array does not have the same number of samples in all channels");
        }
        data.add(samples);
        length += samples[0].length;
    }
    
    public float[][] toArray() {
        if (length == 0)
            return new float[1][0];
        float[][] result = new float[nbChannels][length];
        int offset = 0;
        for (float[][] block : data) {
            for (int chan = 0; chan < nbChannels; chan++)
                System.arraycopy(block[chan], 0, result[chan], offset, block[chan].length);
            offset += block[0].length;
        }
        return result;
    }

    public void close() {

    }

}
