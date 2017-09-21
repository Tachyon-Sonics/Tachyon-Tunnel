package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Indicate that the parameter has a minimum, maximum and a default value, but these
 * values are not fixed and may change from one processing to another.
 * <p>
 * When this annotation is used on a parameter, it is the responsibility of the plugin
 * to adjust values that are out of range. Values smaller than the minimum must set
 * the parameter to the minimum, and values higher than the maximum must set the parameter
 * to the maximum.
 * <p>
 * If the minimum, maximum and default values are fixed, you should use {@link Range} instead.
 * If they depend on the sampling rate, you may consider {@link SampleRateTransform} as well.
 * <p>
 * Unlike {@link Range}, this annotation does not specify the minimum, maximum and default
 * values. Instead the following handshake is performed by the engine
 * (just after {@link ILoadUnload#load(ch.tachyon.tunnel.plugin.IProcessingInfo)} if implemented)
 * in order to transfer the minimum, maximum and default values to the host:
 * <ul>
 * <li>The engine gets the parameter value and consider the result as the default value
 * <li>The engine sets the parameter with the maximum value of the parameter's data type
 * ({@link Long#MAX_VALUE}, {@link Float#POSITIVE_INFINITY}, etc), gets back the value
 * and consider the result as the maximum
 * <li>The engine sets the parameter with the minimum value of the parameter's data type
 * ({@link Long#MIN_VALUE}, {@link Float#NEGATIVE_INFINITY}, etc), gets back the value
 * and consider the result as the minimum
 * <li>The engine sets back the parameter to the default value retrieved at the first step.
 * </ul>
 * <p>
 * This annotation should be put on the getter method of the parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VariableRange {

}
