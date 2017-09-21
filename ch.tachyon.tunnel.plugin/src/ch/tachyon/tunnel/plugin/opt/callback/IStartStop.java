package ch.tachyon.tunnel.plugin.opt.callback;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * Interface for plugins that need to know when processing starts and stops.
 */
public interface IStartStop {

    /**
     * Starts processing.
     * <p>
     * This method is invoked before the processing starts. No parameter will be set after this method
     * is invoked until {@link #stopProcessing()} is invoked (except {@link Sweepable} parameters).
     * <p>
     * The host or engine may still invoke a few processing-related methods before actually processing
     * the first audio chunk, such as {@link ILatency#getLatency(IoDirection)},
     * {@link IFixedChunkLength#getFixedChunkLength()}, etc. Use {@link IBeginProcessing} to be notified
     * just before processing the first audio chunk.
     * <p>
     * When several instances are used for parallel processing, this method is invoked on every instance
     * sequentially (no two instances will have this method invoked concurrently).
     * @param info processing informations such as sample rate, number of channels, etc.
     * @see IBeginProcessing#beginProcessing(IProcessingInfo)
     */
    public void startProcessing(IProcessingInfo info);

    /**
     * Stop processing. Invoked after an audio chunk or file has been fully processed.
     * <p>
     * After this method, the value of the parameters might be changed and
     * {@link #startProcessing(IProcessingInfo)} can be invoked again on other audio, or the effect will
     * be released.
     * <p>
     * Effects for which the result of processing a sample depends on the preceding samples (such as reverberation)
     * may reset their state at this point. The idea is to prevent the signal content from the last processed audio chunk
     * to reverb inside the beginning of the next one.
     * <p>
     * When several instances are used for parallel processing, this method is invoked on every instance
     * sequentially (no two instances will have this method invoked concurrently).
     * <p>
     * Unlike {@link #startProcessing(IProcessingInfo)}, but like {@link IBeginProcessing#beginProcessing(IProcessingInfo)},
     * this method is invoked by the Thread that has called the <code>process()</code> method on this instance.
     */
    public void stopProcessing();

}
