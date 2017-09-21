package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;

/**
 * Indicate that a change of the value of this parameter while processing will require the host
 * to again skip initial samples on the next block of audio data, as reported by
 * {@link ILatency#getLatency(IoDirection)}.
 * This annotation is rarely used.
 * <p>
 * This annotation only makes sense on an effect that has a non zero latency, as reported by
 * {@link ILatency#getLatency(IoDirection)}; and that is sweepable, as indicated by {@link Sweepable}.
 * This annotation is not used by effects based on {@link IMultiChanPushEffect} or {@link IMultiChanPullEffect} as such
 * effects must handle latency issues themselves.
 * <p>
 * Use it if a parameter is sweepable but needs to rebuild internal buffer. An example could be the
 * FFT size parameter, if you want to make such a parameter {@link Sweepable}. Note that when such
 * a parameter is modified while processing, the host will usually call {@link ILatency#getLatency(IoDirection)}
 * again before continuing the processing, in case the latency has changed.
 * <p>
 * The annotation is rarely used in practice, and it should be used with care, as it mean that each
 * change of the value of the parameter will require subsequent additional audio processing.
 * <p>
 * This annotation should be put on the getter method of the parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResetProcessing {

}
