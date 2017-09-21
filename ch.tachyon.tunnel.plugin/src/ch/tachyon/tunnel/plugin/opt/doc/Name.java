package ch.tachyon.tunnel.plugin.opt.doc;

import java.lang.annotation.*;

/**
 * Specify the displayed name of this plugin or parameter.
 * <p>
 * In the absence of this annotation, the name of a plugin is given by the class name
 * and the name of a parameter is given by the property name (in PascalCase).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {

    public String value();

}
