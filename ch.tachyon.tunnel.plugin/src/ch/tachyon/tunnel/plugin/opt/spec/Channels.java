package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Indicate the number of channels the plugin is able to process.
 * The plugin must either specify a minimum and a maximum, or a list of individual values.
 * The list of individual values, if specified (not empty), superceeds the minimum and maximum values.
 * <p>
 * This annotation must be placed on the effect class. If this annotation is not present, it is assumed
 * that the effect can process any number of channels.
 * <p>
 * This annotation is only relevant for effects implementing a multi-channel interface such as
 * {@link IMultiChanEffect} or {@link IMultiChanPullEffect}.
 * @see IStartStop#startProcessing(IProcessingInfo)
 * @see IProcessingInfo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Channels {

    public int min() default 1;

    public int max() default Integer.MAX_VALUE;

    public int[] values() default {};

}
