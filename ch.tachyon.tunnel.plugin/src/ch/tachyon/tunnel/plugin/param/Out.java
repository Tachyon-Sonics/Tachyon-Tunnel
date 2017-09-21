package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Indicate this is an "out" parameter. Writing has no effect, and the value returned by a read
 * is produced by the plugin.
 * <p>
 * A parameter that has this annotation corresponds to the result of an analysis. It is
 * not editable by the user.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Out {

}
