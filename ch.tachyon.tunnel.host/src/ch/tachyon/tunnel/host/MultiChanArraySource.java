package ch.tachyon.tunnel.host;

import ch.tachyon.tunnel.common.*;

/**
 * {@link IMultiChanAudioSource} implementation that reads from an array representing the whole audio
 */
public class MultiChanArraySource implements IMultiChanAudioSource {
    
    private final float[][] array;
    private final AudioStreamFormat format;
    private int offset;
    

    public MultiChanArraySource(float[][] array, double sampleRate) {
        this(array, sampleRate, 24);
    }
    
    public MultiChanArraySource(float[][] array, double sampleRate, int precision) {
        this.array = array;
        this.format = new AudioStreamFormat((float) sampleRate, array.length, precision);
        int nbFrames = 0;
        for (int chan = 0; chan < nbFrames; chan++) {
            if (chan == 0)
                nbFrames = array[0].length;
            else
                if (nbFrames != array[chan].length)
                    throw new IllegalArgumentException("The number of samples must be the same in all channels");
        }
    }

    public AudioStreamFormat getAudioFormat() {
        return format;
    }

    public int readSamples(float[][] target) {
        int amount = Math.min(array[0].length - offset, target[0].length);
        if (amount == 0)
            return 0;
        for (int chan = 0; chan < array.length; chan++)
            System.arraycopy(array[chan], offset, target[chan], 0, amount);
        offset += amount;
        return amount;
    }

    public void close() {
        
    }

}
