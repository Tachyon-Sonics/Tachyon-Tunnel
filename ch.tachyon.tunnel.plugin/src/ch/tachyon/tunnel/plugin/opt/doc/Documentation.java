package ch.tachyon.tunnel.plugin.opt.doc;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Documentation {

    /**
     * The path to a resource (that can be obtained using {@link Class#getResource(String)})
     * containing the documentation for this plugin.
     * <p>
     * The documentation must be in HTML format with UTF-8 encoding.
     */
    public String value();

}
