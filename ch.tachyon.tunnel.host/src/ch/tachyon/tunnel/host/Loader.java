package ch.tachyon.tunnel.host;

import java.io.*;
import java.net.*;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

/**
 * Central class for loading plugins dynamically from a host
 * <p>
 * This class is used when plugins are loaded dynamically from a .jar file that is not
 * included in the current class path. To load plugin statically (whose .jar file is
 * included in the path and whose class is known), use {@link StaticLoader}.
 * @author JUILLERAT
 *
 */
public class Loader {
    
    /**
     * Create a new plugin bundle using the given .jar file.
     * The plugins are not loaded yet.
     * @param jarFile the .jar file from which the plugins will be loaded. This can also be a directory of
     * .class files.
     * @param external <code>true</code> if the .jar file is not in the class path, <code>false</code> else.
     * In general, this value is <code>true</code>: the host discovers plugin bundles from a "plugins" directory,
     * and hence the .jar files are not in the class path because they are not known until run time.
     * <p>
     * <b>Warning</b>: if the plugin is in the class path (which is not typical), you must pass <code>false</code> to ensure the current
     * class loader is used to load the plugin.
     * <p>
     * If you pass <code>true</code> and the plugin <i>is</i> in the class path, strange situations may occur,
     * for instance {@link IParameter#setValue(Object, IProcessingInfo) IParameter.setValue()}
     * or {@link IEffect#setParameterValue(ch.tachyon.tunnel.common.ParameterDefinition, Object)
     * IEffect.setParameterValue()} may fail with a {@link ClassCastException} because you pass a value
     * loaded with the current class loader to a plugin loaded with a different class loader.
     * <p>
     * Conversely, if you pass <code>false</code> and the plugin <i>is not</i> in the class path, you may encounter various
     * problems that may not show up on simple situations but may occur when many plugins are used. More precisely,
     * if two plugins use different versions of the same library, that library will be loaded only once, potentially
     * breaking one of the two plugins. Passing <code>true</code> ensures that the two versions are independently
     * loaded, by using multiple {@link ClassLoader}s.
     * <p>
     * In general, if the plugin is already in the class path, you should use {@link StaticLoader} instead,
     * which allows you to load a plugin using its {@link Class} rather than using its name.
     */
    public static IPluginBundle loadPluginBundle(File jarFile, boolean external) {
        return EngineResolver.getEngine().loadPluginBundle(jarFile, external);
    }
    
    /**
     * @see #loadPluginBundle(File, boolean)
     */
    public static IPluginBundle loadPluginBundle(URL jarUrl, boolean external) {
        return EngineResolver.getEngine().loadPluginBundle(jarUrl, external);
    }

}
