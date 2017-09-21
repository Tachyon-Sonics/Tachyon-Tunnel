package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Indicates the sample rates a plugin is able to handle. This annotation must be placed on the plugin class.
 * <p>
 * The plugin must either specify a minimum and a maximum, or a list of individual values.
 * The list of individual values, if specified (not empty), superceeds the minimum and maximum values.
 * <p>
 * This annotation must be placed on the effect class.
 * @see IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)
 * @see IProcessingInfo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SampleRates {

    public float min() default 0.0f;

    public float max() default Float.POSITIVE_INFINITY;

    public float[] values() default {};

}
