package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.opt.doc.*;

/**
 * Indicate the unit for the parameter. A host may choose to display the given string
 * just after the label or after the input field.
 * <p>
 * Any string can be used but it should be kept short. Abbreviation should be used instead
 * of full names. Use the {@link Description} annotation to provide more information if
 * desired.
 * <p>
 * Possible examples are "<code>dB</code>", "<code>Hz</code>", "<code>%</code>", etc.
 * Do not specify any unit for a parameter that corresponds to a ratio not given in percentage.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Unit {

    /**
     * The unit (abbreviation) for this port's value
     */
    public String value() default "";

}
