package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Reports that the plugin has a non-zero latency.
 */
public interface ILatency {

    /**
     * The latency of this effect. The returned value corresponds to the number of initial output samples
     * the host has to skip, and to the number of zero samples that must be appended to the input.
     * <p>
     * This method is invoked just after {@link IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}. The host
     * or engine will use the returned value to compensate the latency if relevent.
     * @param ioDirection whether to return the input or output latency. In most case the values are the
     * same and the parameter can be ignored. This parameter is not relevant for effects that do not change
     * the audio speed, and <code>null</code> can be used.
     * <ul>
     * <li>{@link IoDirection#OUTPUT} specifies how many initial output samples must be skipped.
     * <li>{@link IoDirection#INPUT} specified how many zeroes must be appended to the input in order to get
     * the full result
     * </ul>
     * The two values may not be the same for a plugin that changes the audio speed. A <code>null</code> should
     * be interpreted like {@link IoDirection#OUTPUT}.
     */
    public double getLatency(IoDirection ioDirection); // WAIT remove arg and add new interface ISpeedChanger

}
