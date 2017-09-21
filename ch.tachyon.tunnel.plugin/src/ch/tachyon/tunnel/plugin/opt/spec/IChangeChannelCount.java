package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Indicate that the plugin changes the number of audio channels as a result of the processing.
 * <p>
 * This interface can only be used if the plugin implements a multi-channel interface. It cannot be
 * used with the single-channel interfaces {@link ISimpleEffect}, {@link IPullEffect} and
 * {@link IPushEffect}.
 * <p>
 * The number of input channel is always supplied by the host (see {@link IProcessingInfo#getNumberOfChannels()}).
 * This interface only allows the plugin to specify the resulting number of output channels. If only
 * some numbers of input channels are supported, the {@link Channels} annotation can be used together with
 * this interface.
 * <p>
 * The returned number of output channels is not allowed to change during the processing. It
 * can, however, depend on the plugin's parameter and number of input channels. The method will be invoked after
 * {@link IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}, but before
 * processing any audio frame.
 * <p>
 * Note:
 * <ul>
 * <li>With the {@link IMultiChanEffect} interface, the argument will have as many channels as the maximum between
 * input and output. Extra channels can be ignored as appropriate for either input or output. With
 * the other interfaces such as {@link IMultiChanPullEffect} and {@link IMultiChanPushEffect}, different objects are
 * used for input and output and they must hold the corresponding number of channels.
 * <li>To create a "generator" or "instrument" plugin (for example a plugin that generates some signal, tone or noise), todo
 * <li>0 channel is invalid for both input or output.
 * </ul>
 */
public interface IChangeChannelCount {

    /**
     * Get the number of output channel that will result after the processing.
     * @param info info about the audio to process. {@link IProcessingInfo#getNumberOfChannels()}
     * can be used to get the number of input channels
     * @return the number of output channels
     */
    public int getNbOutputChannels(IProcessingInfo info);

}
