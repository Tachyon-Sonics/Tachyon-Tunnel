package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Indicate that the value held by this parameter must be transformed to obtain
 * the value that is effectively displayed by the GUI.
 * <p>
 * The value is a class implementing {@link IParameterTransform} that does
 * the transformation from the stored value to the displayed value.
 * <p>
 * The getter and setter for the parameter use the stored (untransformed) values. The GUI
 * (and usually the effect) use the displayed (transformed) value.
 * <p>
 * Two examples implemented in this package are:
 * <ul>
 * <li>{@link PowerOfTwoTransform} is used to store a value as an integer, and to use the
 * corresponding power of two as the actual and displayed value. Hence, stored values 3, 4, 5 for example correspond
 * to the displayed values 8, 16 and 32. This can be used to ensure only powers of two can be used.
 * <li> {@link SampleRateTransform} is used to store a real value
 * and to use the value multiplied by the current sample rate as the displayed value. A stored value
 * between 0 and 0.5 for example corresponds to a displayed frequency between 0 and Nyquist.
 * </ul>
 * The minimum, maximum and default value are also transformed if specified by {@link Range}.
 * Hence their value must correspond to the stored (untransformed) values.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transform {

    public Class<? extends IParameterTransform> value();

}
