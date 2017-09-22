package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.plugin.*;

/**
 * Indicate that the effect requires some samples before and/or after the audio chunk to process.
 * <p>
 * This can be used for example by effects that use interpolation of several consecutive samples.
 * <p>
 * The effect will receive the additionnal requested samples and must output a corresponding number
 * of output samples; however the corresponding output samples will be ignored.
 * <p>
 * If there are no samples available before/after the audio chunk to process (for instance when
 * processing an entire file), either:
 * <ul>
 * <li>Samples with a value of zero will be received if the requested number is negative (the absolute value is used)
 * <li>No additionnal samples (or only as much as available if less than requested) will be received
 * if the requested number is positive.
 * </ul>
 */
public interface IRequiredBeforeAfter {

    /**
     * The number of required samples before the audio chunk to process. If negative, the
     * absolute value is used, and zero samples are used even if no samples are available.
     * If positive, samples before the audio chunk will only be received if available.
     */
    public long getRequiredSamplesBefore(IProcessingInfo info);

    /**
     * The number of required samples after the audio chunk to process. If negative, the
     * absolute value is used, and zero samples are used even if no samples are available.
     * If positive, samples after the audio chunk will only be received if available.
     * <p>
     * If {@link ILatency} is also implemented, the number of additionnal samples will be the
     * maximum between {@link ILatency#getLatency(ch.tachyon.tunnel.common.IoDirection)} and
     * the result of this method. If the latency is greater, first samples after the audio chunk
     * are received as specified by this method, and then zero samples until the latency.
     */
    public long getRequiredSamplesAfter(IProcessingInfo info);

}
