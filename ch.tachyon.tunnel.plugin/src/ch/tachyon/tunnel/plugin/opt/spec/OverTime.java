package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OverTime {

    /**
     * Java name of the parameter, camel-case, without "get" or "set".
     * <p>
     * For example, if the getter is <tt>getQuality</tt>, use "<code>quality</code>"
     */
    public String parameterName();

    public double minValue();

}
