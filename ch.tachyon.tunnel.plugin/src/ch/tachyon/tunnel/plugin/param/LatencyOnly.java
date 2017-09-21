package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Indicate that the parameter only affects the lateny (and possibly the processing speed), but has
 * no effect on the result.
 * <p>
 * More precisely, if all other parameters have given values, this annotation indicates that the result
 * of the processing will be the same (bit by bit) regardless of the value of this parameter (with the
 * assumption that the host compensates for the latency).
 * <p>
 * A host that does not do <i>live</i> processing (from microphone to loudspeaker in real-time) may choose
 * not to display the parameter to the user, and to use the optimal value given by {@link #speedOptimalValue()}
 * instead.
 * <p>
 * A host that does <i>live</i> processing might display the parameter and use the optimal value given
 * by {@link #latencyOptimalValue()} by default.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LatencyOnly {

    /**
     * The value of the annotated parameter that is optimal in terms of processing speed, in a situation
     * where latency can be compsensated (such as an audio editor) or is not important (such as
     * an audio player).
     * <p>
     * A host that choose not to display this parameter to the user should set the parameter
     * to that value before the processing
     * <p>
     * A value of {@link Double#NaN} means that there is no optimal default value, and the value
     * specified by {@link Range#defaultValue()} should be used.
     */
    public double speedOptimalValue() default Double.NaN;

    /**
     * The value of the annotated parameter that is optimal in terms of low latency, in a situation
     * where latency cannot be compensated, such as in a <i>live</i> processing.
     * <p>
     * A value of {@link Double#NaN} means that there is no optimal default value, and the value
     * specified by {@link Range#defaultValue()} should be used.
     */
    public double latencyOptimalValue() default Double.NaN;

}
