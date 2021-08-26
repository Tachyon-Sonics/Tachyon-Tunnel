package ch.tachyon.tunnel.common;

import javax.sound.sampled.*;

/**
 * A multi-channel audio source that can be read from in multiple chunks using
 * <tt>float[][]</tt> arrays.
 */
public interface IMultiChanAudioSource {
    
    /**
     * Get the audio format of the source, such as number of channels and sample rate.
     * <p>
     * While encoding, bit depth and endianess can be specified, they correspond to the "original" format before decoding.
     * But they are of no use for processing as the stream is always converted to Java <tt>float</tt>s
     * (as passed to the {@link #readSamples(float[][])} method) between <tt>-1</tt> and <tt>1</tt>, corresponding to the raw PCM signal.
     */
    public AudioFormat getAudioFormat();
    
    /**
     * Read as much sample frames as available to fill the given target array.
     * <p>
     * The first dimension of the array corresponds to the audio channel, and its size must match the number of channels
     * of this source ({@link #getAudioFormat()}.{@link AudioFormat#getChannels() getChannels()}). The second dimension
     * corresponds to the samples and must have the same size in all channels. As a typical example, <tt>float[2][2048]</tt>
     * allows you to read 2048 sample frames (at a time) of a stereo (2 channels) audio stream.
     * <p>
     * This method will always fill the target array, unless the end of the audio stream has been reached.
     * Hence, the returned value will be samller that the number of sample frames of the array (<tt>target[0].length</tt>)
     * if and only if the last sample frames have just been read. Subsequent calls to this method after the last sample has been read
     * will return 0.
     * @param target the array to fill with the sample frames read.
     * @return the number of sample frames read. If it is smaller that <tt>target[0].length</tt> the end of the audio stream has
     * been reached, and the returned sample frames are the last sample frames.
     */
    public int readSamples(float[][] target);

    /**
     * Close this audio source. A source must always be closed after you have finished working with it.
     * This method will release any resource associated to the source. This might mean closing the file
     * if the source comes from a file, or closing a hardware audio input port if the source comes from
     * hardware input, or anything else.
     * @throws RuntimeException (UncheckedIOException on Java 8 or greater) if there are any I/O error closing this source
     */
    public void close();

}
