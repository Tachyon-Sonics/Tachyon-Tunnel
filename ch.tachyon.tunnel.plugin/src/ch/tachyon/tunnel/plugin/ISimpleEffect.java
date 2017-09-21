package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.plugin.opt.spec.*;


/**
 * Interface for simple plugins that can process audio by chunks, and one channel at a time.
 * This is the recommanded interface plugins should implement whenever possible.
 * <p>
 * This interface cannot be used in the following cases:
 * <ul>
 * <li>If the plugin must process all channels at the same time (for example to synchronize the resulting
 * phase between the audio channels). In that case, {@link IMultiChanEffect} can be used.
 * <li>If the plugin changes the speed of the audio (for example a resampling or time-stretching plugin)
 * or more generally if the plugin needs to read and write locally or globally at different rates.
 * In that case, {@link IPullEffect} or {@link IPushEffect} can be used.
 * <li>If the plugin changes the number of channels, {@link IMultiChanEffect} and {@link IChangeChannelCount}
 * must be used.
 * <li>If the plugin must access the input audio in a random access fashion. In that case {@link IRandomAccessEffect} can be used.
 * </ul>
 */
public interface ISimpleEffect extends IPlugin {

    /**
     * Process the given chunk of monophonic audio data, in place. The plugin should expect the size
     * of the chunk to vary from one call to another.
     */
    public void process(float[] data);

}
