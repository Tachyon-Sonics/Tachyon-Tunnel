package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;


/**
 * Specifies that the plugin can only accept audio chunks of a given length during
 * processing.
 * <p>
 * The {@link #getFixedChunkLength()} method is invoked after the parameters have been set
 * and before the processing starts. The returned value may depend on the value of the
 * parameters but is not allowed to change during the processing.
 * <p>
 * If the effect also implements {@link IStartStop}, {@link #getFixedChunkLength()}
 * is invoked after
 * {@link IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}
 * but before the first chunk is processed.
 * <p>
 * If the plugin accepts various lengths (such as, for example, all multiples of 512),
 * it is a good practice to choose and return the one that is the closest to the
 * host's desired one, because the later may correspond to a latency target chosen by the user.
 * The host preferred length can be inquired using
 * {@link IProcessingInfo#getHostPreferredChunkLength()} on the argument passed
 * to {@link IStartStop#startProcessing(IProcessingInfo)}. Observe that the host
 * may return <code>-1</code> if it has no preferrence.
 */
public interface IFixedChunkLength {

    /**
     * The chunk length (in sample frames) this plugin expects.
     * <p>
     * The plugin will receive audio chunks of the returned length during the processing, even
     * if the host use another length. The engine will automatically perform rebuffering if
     * the host and plugin do not agree on a common length.
     * <p>
     * If the plugin changes the audio duration, the length returned by this method is the one of
     * the array passed to the <code>process</code> method. This is for instance the
     * input length for {@link IPushEffect}, and the output length for {@link IPullEffect}.
     */
    public int getFixedChunkLength();

}
