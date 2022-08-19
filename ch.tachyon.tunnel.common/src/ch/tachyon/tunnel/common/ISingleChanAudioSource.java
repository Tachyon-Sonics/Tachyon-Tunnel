package ch.tachyon.tunnel.common;

import java.io.*;

/**
 * Interface for reading consecutive audio samples by blocks represented by a <code>float</code> array.
 * <p>
 * This interface is used for single-channel audio. For multi-channel audio, {@link IMultiChanAudioSource}
 * must be used
 */
public interface ISingleChanAudioSource extends Closeable {
    
    /**
     * Get the audio format of the source, such as the sample rate.
     * <p>
     * While encoding, bit depth and endianess can be specified, they correspond to the "original" format before decoding.
     * But they are of no use for processing as the stream is always converted to Java <tt>float</tt>s
     * (as passed to the {@link #readSamples(float[])} method) between <tt>-1</tt> and <tt>1</tt>, corresponding to the raw PCM signal.
     * <p>
     * The resulting {@link AudioStreamFormat} must have one channel.
     */
    public AudioStreamFormat getAudioFormat();

    /**
     * Read the next block of audio samples into the given array.
     * <p>
     * The implementation must always read as much samples as specified by the array length,
     * unless the end of the audio stream has been reached. The implementation must also handle the
     * case in which one or more invocations are performed after the end of the audio stream,
     * by returning 0.
     * @param target the array to fill with the read audio samples
     * @return the number of samples that could actually be read. This value is only less than the array
     * size when the end of the audio stream has been reached.
     */
    public int readSamples(float[] target);

    /**
     * Close this audio source. A source must always be closed after you have finished working with it.
     * This method will release any resource associated to the source. This might mean closing the file
     * if the source comes from a file, or closing a hardware audio input port if the source comes from
     * hardware input, or anything else.
     * @throws RuntimeException (UncheckedIOException on Java 8 or greater) if there are any I/O error closing this source
     */
    public void close();

}
