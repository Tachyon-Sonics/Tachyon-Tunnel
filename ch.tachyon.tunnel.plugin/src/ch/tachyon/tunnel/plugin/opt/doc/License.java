package ch.tachyon.tunnel.plugin.opt.doc;

import java.lang.annotation.*;

/**
 * Indicates the author and license for a plugin. The value is a class that can be instancied using a
 * public no-arg constructor, and implements the {@link ILicensed} interface.
 * <p>
 * This annotation must be placed on the plugin class.
 * <p>
 * This annotation can be used in place of the {@link ILicensed} interface in case several plugins
 * use the same license.
 * <p>
 * Hosts should honor this annotation by providing a way for the user to read all the
 * information provided by this annotation for any plugin that has it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface License {

    public Class<? extends ILicensed> value();

}
