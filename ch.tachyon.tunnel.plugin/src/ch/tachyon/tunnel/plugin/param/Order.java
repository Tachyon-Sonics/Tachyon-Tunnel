package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

/**
 * Specify the order in which parameters should be displayed in the GUI.
 * Parameter with a smaller {@link #value()} are displayed first.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

    public int value();

}
