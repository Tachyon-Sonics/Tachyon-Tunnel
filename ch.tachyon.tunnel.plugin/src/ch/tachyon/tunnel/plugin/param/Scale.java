package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Indicate the scale to use when the parameter's value is displayed by a slider.
 * Possible values are:
 * <ul>
 * <li>{@link ScaleType#LINEAR}, this is the default value. Pixel increments correspond to proportional increments
 * of the value.
 * <li>{@link ScaleType#LOGARITHMIC}: pixel increments correspond to proportional multiplication of the value. Use this
 * for a parameter that is a ratio and that is not expressed in a logarithmic unit such as decibels. This option
 * is only valid if the parameter's min and max value are both strictly positive or both strictly negative.
 * Else, {@link ScaleType#CUBEROOT} will be used instead.
 * <li>{@link ScaleType#CUBEROOT} can be used if small and large values are possible, but values around
 * zero are the most relevant one.
 * <li>{@link ScaleType#CUBE} can be used to favor extreme values over values near zero. It is rarely useful.
 * </ul>
 * Note that if the value is already in a logarithmic unit such as dB, {@link ScaleType#LINEAR} should be used
 * to avoid a "double logarithmic" scale.
 * <p>
 * In addition, {@link #steps()} allows you to define the number of "steps" between the minimum and maximum
 * values. A "step" corresponds to an increment or decrement when clicking on the left or right of the slider.
 * The default value is <code>10</code>. To ensure consistency, avoid using values significantly less than
 * <code>5</code> or greater than <code>20</code>. The host generated GUI may ignore this value.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scale {

    /**
     * The type of scale for the annotated parameter
     */
    public ScaleType value();

    /**
     * The number of steps for the annotated parameter
     */
    public int steps() default 10;

}
