package ch.tachyon.tunnel.common;

import java.io.*;

/**
 * Multi-channel version of {@link ISingleChanAudioSink}
 */
public interface IMultiChanAudioSink extends Closeable {

    /**
     * Multi-channel version of {@link ISingleChanAudioSink#writeSamples(float[])}
     * @param samples
     */
    public void writeSamples(float[][] samples);

    /**
     * Close this audio sink. A sink must always be closed after you have finished working with it.
     * This method will release any resource associated to the sink. This might mean closing the file
     * if the sink writes to a file, or closing a hardware audio output port if the sink plays through
     * an audio output port, or anything else.
     * @throws RuntimeException (UncheckedIOException on Java 8 or greater) if there are any I/O error closing this sink
     */
    public void close();

}
