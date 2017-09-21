package ch.tachyon.tunnel.plugin.opt.doc;

import java.lang.annotation.*;

/**
 * Specify a short description for this plugin or parameter.
 * The description must consist of a few words, optionally followed by a return and a few lines of details.
 * <p>
 * Example:
 * <pre>
 * Change the speed without affecting the pitch.\n
 * Implemented using a phase vocoder algorithm.
 * </pre>
 * The host may choose to display the first line only in some cases, and the whole text (if more than
 * one line) in other cases.
 * <p>
 * Hosts are encouraged to optionally use the description in addition to the plugin/parameter name (as
 * specified by {@link Name}} if they implement a "search" feature.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {

    public String value() default "";

}
