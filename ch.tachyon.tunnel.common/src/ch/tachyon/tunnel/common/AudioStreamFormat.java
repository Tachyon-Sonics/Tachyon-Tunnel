package ch.tachyon.tunnel.common;


/**
 * Format of an audio stream (float[][]) as used by {@link IMultiChanAudioSource} and {@link IMultiChanAudioSink}.
 * <p>
 * This class is used in place of javax.sound.sampled.AudioFormat so that we do not depend on it in this project.
 * <p>
 * For the rare case in which it is necessray to recover information from the original javax.sound.sampled.AudioFormat,
 * it can be passed to {@link #setAudioFormat(Object)} and read back by {@link #getAudioFormat()}.
 * A cast is necessary as we declare it as Object here to prevent any hard depedency.
 */
public class AudioStreamFormat {

    private final float sampleRate;
    private final int channels;
    private final int quantization;
    private Object audioFormat;


    /**
     * @param sampleRate sample rate in Hz
     * @param nbChans number of channels. 1 = mono, 2 = stereo, etc
     * @param quantization precision in bits, for instance <tt>16</tt> for 16 bit. Use 32 for IEEE floats.
     * This is a hint based on the original source, even when only 32 bit <tt>float</tt>s are used internally for processing
     */
    public AudioStreamFormat(float sampleRate, int nbChans, int quantization) {
        this.sampleRate = sampleRate;
        this.channels = nbChans;
        this.quantization = quantization;
    }
    
    /**
     * @return sample rate in Hz
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * @return number of channels. 1 = mono, 2 = stereo, etc.
     */
    public int getChannels() {
        return channels;
    }
    
    /**
     * @return precision in bits, for instance <tt>16</tt> for 16 bit or 32 for IEEE floats
     */
    public int getQuantization() {
        return quantization;
    }

    /**
     * The underlying javax.sound.sampled.AudioFormat object from which this instance was created, if any
     */
    public Object getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(Object audioFormat) {
        this.audioFormat = audioFormat;
    }

}
