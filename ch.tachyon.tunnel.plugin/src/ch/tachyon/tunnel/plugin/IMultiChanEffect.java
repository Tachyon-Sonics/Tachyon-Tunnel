package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Interface for simple plugins that can process multi-channel audio by chunks. Each chunk is an
 * two dimensional array of floats, corresponding to a chunk of samples of each channel.
 * <p>
 * This interface should only be used for plugins that require all the audio channels at the
 * same time. Most plugins can process each channel independently and should hence use {@link ISimpleEffect}
 * instead.
 */
public interface IMultiChanEffect extends IPlugin {

    /**
     * Process the given chunk of audio data in place.
     * <p>
     * If the plugin implements the {@link IStartStop} interface, it can safely assume that the number
     * of channels will be the same for all invocations of this method bewteen
     * {@link IStartStop#startProcessing(IProcessingInfo)} and {@link IStartStop#stopProcessing()}.
     * However the number of samples can change from one invocation to the next.
     * @param data the chunk of audio data. First dimension is channel number, second one is sample index
     */
    public void process(float[][] data);

}
