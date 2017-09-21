package ch.tachyon.tunnel.host;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Utilities to load plugins statically. This class is only used to load plugins that are
 * already in the class path and whose classes are known.
 * <p>
 * To load plugins dynamically from external .jar files that may not be included in the class path,
 * the {@link Loader} and {@link IPluginBundle} classes must be used.
 */
public class StaticLoader {

    /**
     * Load a plugin statically. Unlike {@link Loader#loadPluginBundle(java.io.File, boolean)} that is used
     * to load plugins dynamically from an external .jar file, this method is used to load a plugin
     * whose class is known and is present in the current class path.
     * @param <I> the interface you want to use to communicate with the plugin
     * @param <E> the plugin class
     * @param commIntf the interface you want to use to communicate with the plugin
     * @param pluginClass the plugin class
     * @return the requested plugin, wrapped in the requested interface
     */
    public static <I extends IEffect, E extends IPlugin> I getPlugin(Class<I> commIntf, Class<E> pluginClass) {
        return EngineResolver.getEngine().getStaticPlugin(commIntf, pluginClass);
    }

    public static IMtContext getMtcontext() {
        return EngineResolver.getEngine().getStaticMtcontext();
    }

}
