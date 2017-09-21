package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Audio resampler (or sample rate converter).
 * Concrete implementations are constructued with an {@link IAudioSource} object that is
 * responsible for suppling the input audio samples in chunks (as arrays of float).
 * The {@link ResamplerFactory} can be used to create concrete implementations in a simplified way.
 * <p>
 * This interface allows you to resample audio in chunks, using a PULL approach (you specify the
 * output chunks to be produced, and input chunks are retrieved as needed):
 * <pre>
 * IAudioSource source = new {@link IAudioSource}() {...}; // Implement it!
 * IResampler resampler = new <i>ResamplerClass</i>(source, ...);
 * float[] chunk = new float[<i>chunk_size</i>];
 * int count = 0;
 * do {
 *     count = resampler.resample(chunk);
 *     ... // Play / store samples from chunk[0] to chunk[count - 1]
 *     // Note that count == chunk.length, unless the end of the source audio stream is reached
 * } while (count == chunk.length);
 * </pre>
 * Input audio chunks are retrieved from the input stream as needed using the supplied {@link IAudioSource}
 * when {@link #resample(float[]) resample} is invoked.
 * <p>
 * Most {@link IResampler} implementation have a non-zero latency, meaning that the output is slightly
 * delayed with respect to the input. Use {@link #getLatency()} to get the latency.
 */
public interface IResampler {

    /**
     * Produce a chunk of audio by resampling audio supplied from the {@link IAudioSource} object
     * specified in the constructor. As much audio chunks as needed are read from the {@link IAudioSource}
     * object to fill the entire <code>output</code> array.
     * <p>
     * To process a large audio stream, divide it in blocks and invoke this method on each block.
     * @param output the array to fill with the result. Any previous content is ignored.
     * @return the number of samples that are actually relevant; usually the same as <code>output.length</code>,
     * unless the end of the audio stream has been reached. Even if the result is smaller than
     * <code>output.length</code>, the entire array is filled by this method. Depending on the concrete
     * resampling algorithm implementation, samples past the end of the stream may of may not be zeros, but they
     * will always converge toward zero.
     */
    public int resample(float[] output);

    /**
     * This method can be invoked when you have finished processing an audio stream and want to start
     * processing another one (such as another audio channel) using the same parameters.
     * <p>
     * The cost of invoking this method is usually less than the cost of creating a new instance.
     */
    public void reset();

    /**
     * The input latency, or the number of input sample that must be read before the output sample
     * corresponding to the first input sample is produced.
     * <p>
     * The <i>output</i> latency, or the number of output samples that can safely be dropped is given
     * by <code>getLatency() * ratio</code> where <code>ratio</code> is the resampling ratio.
     */
    public int getLatency();

}
