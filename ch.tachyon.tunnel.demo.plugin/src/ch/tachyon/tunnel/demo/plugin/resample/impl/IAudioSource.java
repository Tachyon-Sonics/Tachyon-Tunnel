package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Represent an object that is capable of supplying audio data
 * as sequential chunks (arrays of float).
 */
public interface IAudioSource {

    /**
     * Get the next block of audio data. The first call to this method must return the first chunk of
     * audio data, and successive calls to this method must return successive blocks of audio data.
     * @param target the buffer to fill with the next block of audio data
     * @param offset the offset in the buffer from which to fill data. The implementation must
     * <b>not</b> modify any element before <code>target[offset]</code>
     * @param count the number of elements to fill. The implementation must <b>not</b> modify
     * any element past <code>target[offset + count - 1]</code>. Usually, classes using this
     * interface will always invoke this method with the same <code>count</code>.
     * @return the number of samples that are actually relevant. Must match <code>count</code> unless
     * the end of the stream has been reached. In that case the implementation must still fill
     * <code>count</code> samples, by using zeroes for samples past the end of the stream. Implementations
     * must handle the case in which this method is invoked multiple time past the end of the stream, by
     * returning zero and filling <code>target[offset]</code> to <code>target[offset + count - 1]</code>
     * with zero.
     */
    public int getSamples(float[] target, int offset, int count);

}
