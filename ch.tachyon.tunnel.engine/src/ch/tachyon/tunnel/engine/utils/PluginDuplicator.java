package ch.tachyon.tunnel.engine.utils;

import java.lang.reflect.*;

import ch.tachyon.tunnel.engine.utils.bean.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Static utilities to create a working copy (for multithreading) of a Plugin
 */
public class PluginDuplicator {

    /**
     * Create a working copy of the given plugin.
     */
    public static <E extends IPlugin> E prepareCopy(E plugin, IProcessingInfo info) {
        if (plugin instanceof IDuplicatable) {
            @SuppressWarnings("unchecked")
            E result = (E) ((IDuplicatable) plugin).duplicate(info);
            return result;
        } else if (plugin instanceof Cloneable) {
            // Use the clone() method
            try {
                Method cloneMethod = plugin.getClass().getMethod("clone");
                @SuppressWarnings("unchecked")
                E result = (E) cloneMethod.invoke(plugin);
                return result;
            } catch (Exception ex) {
                throw new IllegalStateException("Plugin " + plugin + " is Cloneable but the clone() method could not be invoked successfully", ex);
            }
        } else {
            // Instanciate and copy state
            E copy;
            try {
                @SuppressWarnings("unchecked")
                E toto = (E) plugin.getClass().newInstance();
                copy = toto;
            } catch (InstantiationException ex) {
                throw new IllegalStateException("Could not instanciate a plugin " + plugin + " with the public no-arg constructor", ex);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Could not instanciate a plugin " + plugin + " with the public no-arg constructor", ex);
            }
            if (copy instanceof ILoadUnload)
                ((ILoadUnload) copy).load(info);
            // Copy parameters
            try {
                for (MemberInfo property : new ClassInfo(plugin.getClass()).getMembers()) {
                    Object value = property.getReader().invoke(plugin);
                    property.getWriter().invoke(copy, value);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot copy plugin's parameter to another instance", ex);
            }
            return copy;
        }
    }

}
