package ch.tachyon.tunnel.host;

import java.util.*;
import ch.tachyon.tunnel.host.effect.*;


/**
 * Information about the host and the audio that is about to be processed.
 * Here, "host" means any code that is using a plugin for processing audio.
 * <p>
 * If the plugin implements <code>IStartStop</code> (see ch.tachyon.tunnel.plugin package), the host will supply an implementation
 * of this interface to <code>IStartStop.startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)</code>, before
 * processing audio data.
 * <p>
 * The plugin may use methods of this interface in the implementation of
 * <code>IStartStop.startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)</code> only.
 */
public interface IProcessingInfo {

    /**
     * Get the number of channels that will be processed by the plugin instance.
     * <p>
     * This may differ from the actual number of channels of the audio data. For example
     * some host may allow the user to select and process only a subset of channels. The
     * returned value is the number of channels that will be processed.
     * <p>
     * If the plugin implements the {@link ISimpleEffect} interface, it will receive one channel
     * at a time and this method always returns <code>1</code>.
     */
    public int getNumberOfChannels();

    /**
     * Get the subset of channel indexes that are being processed.
     * If {@link #getNumberOfChannels()} returns 1, and a stereo file is being processed,
     * this method will return 0 for the instance processing the left channel, and 1 for the
     * instance processing the right channel.
     * <p>
     * Plugins must not assume only channel 0 and 1 exist, 5.1 and 7.1 files may have up to
     * channel index 5 and 7, respectively.
     * <p>
     * For multichannel files, channel
     * indexes are defined as in https://msdn.microsoft.com/en-us/library/windows/hardware/dn653308(v=vs.85).aspx, minus 1 (zero based).
     * In particular, channel index 3 is low frequency (subwoofer) and might need special treatment.
     * Not all channels might be present in a file.
     * <p>
     * The cardinality of the result matches {@link #getNumberOfChannels()}
     */
    public BitSet getChannels();

    /**
     * Get the sample rate of the audio that will be processed, in Hz.
     */
    public float getSampleRate();

    /**
     * Get the number of quantization in bits. For example <code>16</code> for 16 bits (CD quality) or
     * <code>24</code> for 24 bits.
     * <p>
     * When floating-point processing is used, the number of bits of the mantissa is returned, for example
     * <code>24</code> for the <code>float</code> type.
     */
    public int getQuantization();

    /**
     * The maximum audio chunk size (in sample frames) that will be passed to the processing
     * methods. If the host will pass chunks of constant size, this method must return that
     * constant size.
     * <p>
     * This method can base its result on the given plugin optimal chunk length. A host that use
     * chunks of constant size might adapt itself to the plugin optimal chunk length for instance.
     * However it must always return the same value for the same plugin optimal length.
     * <p>
     * Plugins that cannot adapt their chunk size will usually invoke this method. Plugins that
     * can adapt their chunk size will usually invoke {@link #getHostPreferredChunkLength()} followed
     * by {@link #negociateFixedChunkLength(int)}.
     * <p>
     * <b>Warning:</b> there are many internal classes that rely on the returned value in order to
     * create intermediate buffers. Hence as a host developer you <b>must</b> return a relevant value
     * and ensure it is not too large.
     * Do not just return stuff like {@link Integer#MAX_VALUE}, as it would almost certainly throw an
     * {@link OutOfMemoryError}. If the host does not vary the chunk length during processing, the
     * fixed chunk length is returned.
     */
    public int getMaxChunkLength(int pluginOptimalChunkLength);

    /**
     * The host preferred length of the audio chunks (in sample frames), or <code>-1</code> if no particular
     * length is preferred.
     * <p>
     * The given value usually corresponds to a latency target specified by the user or automatically depending
     * on the context (offline processing, player, or live processing).
     * <p>
     * In general, a plugin uses this method to get the host's desired length; then it adjust it to its own
     * requirement if necessary, and invoke {@link #negociateFixedChunkLength(int)} with the adjusted value.
     * If the returned value suits the plugin, it may not invoke {@link #negociateFixedChunkLength(int)} at all.
     * @see #negociateFixedChunkLength(int)
     */
    public int getHostPreferredChunkLength();

    /**
     * Negociate the length of the audio chunks (in sample frames) that will be passed to the processing methods.
     * The argument is the length used by the plugin and the returned value is the length used by the host. If they
     * do not agree, rebuffering will be done automatically by the engine. A return value of
     * <code>-1</code> means the length may vary during the processing. A non-negative value is only
     * returned if the same chunk length is used for the whole processing (between
     * <code>IStartStop.startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)</code> and
     * <code>IStartStop.stopProcessing()</code>).
     * <p>
     * The value returned by the host may or may not take into account the plugin's specified optimal length (as argument).
     * The plugin's specified optimal length may or may not take into account the host's preffered chunk length,
     * as reportd by {@link #getHostPreferredChunkLength()}.
     * <p>
     * The following is an example of typical negotiation for a plugin only accepting multiples of 512:
     * <ul>
     * <li>Plugin invokes {@link #getHostPreferredChunkLength()}
     * <li>Plugin rounds the returned value to the nearest positive multiple of 512, or chooses 512 if -1 was returned
     * <li>Plugin invokes {@link #negociateFixedChunkLength(int)} with the rounded value.
     * <li>The host either returns the same value as {@link #getHostPreferredChunkLength()}, or adapt itself to the plugin and returns
     * the value passed as argument (the value rounded to a multiple of 512).
     * <li>If the host does not return the plugin's length, the engine will automatically perform rebuffering, which will impact latency
     * and may slightly impact performances. If the host return the plugin's length, no rebuffering is done and latency will be optimal
     * </ul>
     * If the host and the plugin do not agree on the chunk length, the engine will automatically rebuffer
     * the audio. Rebuffering can impact processing speed. When the host uses the {@link ch.tachyon.tunnel.host.effect.ISimpleEffect}
     * interface, rebuffering adds latency.
     * @param pluginOptimalChunkLength the length that would be optimal for the plugin, or
     * <code>-1</code> if no particular value is optimal
     */
    public int negociateFixedChunkLength(int pluginOptimalChunkLength);

    /**
     * Get the length of the audio data that will be processed. Can return <code>-1</code> if unknown.
     * <p>
     * The returned length corresponds to the number of input sample frames that will be processed between the calls
     * to <code>IStartStop.startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)</code> and
     * <code>IStartStop.startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)</code>. A "frame" corresponds to <i>n</i> samples,
     * where <i>n</i> is the number of channels.
     */
    public long getLength();

}
