package ch.tachyon.tunnel.common;

import java.io.*;

/**
 * Interface for writing consecutive audio samples by blocks represented by a <code>float</code> array.
 * <p>
 * This interface is used for single-channel audio. For multi-channel audio, {@link IMultiChanAudioSink}
 * must be used.
 */
public interface ISingleChanAudioSink extends Closeable {

    /**
     * Write the next block of audio samples.
     * @param samples the block of audio samples to write
     */
    public void writeSamples(float[] samples);

    /**
     * Close this audio sink. A sink must always be closed after you have finished working with it.
     * This method will release any resource associated to the sink. This might mean closing the file
     * if the sink writes to a file, or closing a hardware audio output port if the sink plays through
     * an audio output port, or anything else.
     * @throws RuntimeException (UncheckedIOException on Java 8 or greater) if there are any I/O error closing this sink
     */
    public void close();

}
