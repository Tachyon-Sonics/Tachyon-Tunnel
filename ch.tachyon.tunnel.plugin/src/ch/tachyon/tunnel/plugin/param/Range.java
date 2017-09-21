package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Indicate the minimum, maximum and default value for a parameter.
 * <p>
 * This annotation use <code>double</code> values, but can also be used on other numeric parameter
 * such as <code>int</code> or <code>long</code>. It can also be used on a <code>boolean</code>
 * parameter, in this case only the {@link #defaultValue()} can be specified, <code>0.0</code> for
 * <code>false</code> and any other value for <code>true</code>. For an enumeration, values
 * must be integers and correspond to the ordinals of the enumeration items.
 * <p>
 * For parameters of enumerated type, {@link #defaultValue()} can be used and is the ordinal of the
 * default enumeration value.
 * <p>
 * When {@link Transform} is used, the values correspond to stored values (untransformed) rather than
 * to the displayed values (transformed).
 * <p>
 * This annotation should be put on the getter method of the parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {

    public double minValue() default 0.0;

    public double maxValue() default 1.0;

    /**
     * The default value, or {@link Double#isNaN()} (default) to means that the default
     * value must be retrieved by reading the parameter's value before any write.
     */
    public double defaultValue() default Double.NaN;

}
