package ch.tachyon.tunnel.host.engine;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.effect.IPullEffect;
import ch.tachyon.tunnel.host.effect.IPushEffect;
import ch.tachyon.tunnel.host.effect.ISimpleEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public interface IPluginBundle {

    /**
     * The {@link ClassLoader} that will be used to load the plugins
     * @param create whether to create it now if it has not been created yet
     */
    public ClassLoader getClassLoader(boolean create);

    /**
     * The current {@link IMtContext}. Gices information about multi-core capabilities
     */
    public IMtContext getMtContext();

    /**
     * Get a string version of the URL from which the plugins are loaded
     */
    public String getUrlString();
    
    /**
     * @return the name of this bundle, as defined by the META-INF/MANIFEST.MF's "Title" entry
     */
    public String getName();

    /**
     * Load all plugins of this bundle that can be bridged to the given interface.
     * <p>
     * The argument specifies how you will interact with the plugins.
     * <p>
     * Common host interfaces are {@link ISimpleEffect}, {@link IPullEffect}, {@link IPushEffect}.
     * <p>
     * Note that the plugins do not need to implement the same interface, any plugin that can be bridged to
     * the requested interface will be wrapped appropriately and returned. In general, the more generic the
     * requested interface, the more plugins you will get. The bridging is done automatically for you.
     * <p>
     * In general, a host should extract the name, description and host interface from the returned list, clear
     * any reference to the returned list or to any of its content,
     * free the bundle using {@link #releaseLoader()}, and reload each plugin for processing only when needed using
     * {@link #loadPlugin(Class, String)}. See {@link #releaseLoader()} for more.
     * @param hostInterface the interface with which you want to interact with the plugins.
     * @return the loaded plugins. You can use <code>instanceof</code> on each plugin if you want to know which
     * of the given host interfaces it implements, so you can later load it with {@link #loadPlugin(Class, String)}.
     */
    public <E extends IEffect> List<E> loadAllPlugins(Class<E> hostInterface) throws IOException;
    
    /**
     * Load all plugins of this bundle that can be bridged to at least one of the given interfaces.
     * <p>
     * The argument is a list of host interfaces, and it specifies how you will interact with the plugins.
     * <p>
     * Common host interfaces are {@link ISimpleEffect}, {@link IPullEffect}, {@link IPushEffect}.
     * <p>
     * Note that the plugins do not need to implement the same interface, any plugin that can be bridged to one of
     * the requested interfaces will be wrapped appropriately and returned. In general, the more generic the
     * requested interface, the more plugins you will get. The bridging is done automatically for you.
     * <p>
     * If multiple interfaces can be bridged, the first one is taken, except if it does not allow multi-threading
     * but another one does (in case the plugin supports multi-threading, and enough CPU cores are available).
     * <p>
     * If both single chan and multi chan interfaces are specified, single chan to multi-chan conversion using
     * parallel threads is only done if the multi chan interface appears first.
     * <p>
     * In most case only one interface is used, in which case {@link #loadAllPlugins(Class)} can be used
     * <p>
     * In general, a host should extract the name, description and host interface from the returned list, clear
     * any reference to the returned list or to any of its content,
     * free the bundle using {@link #releaseLoader()}, and reload each plugin for processing only when needed using
     * {@link #loadPlugin(Class, String)}. See {@link #releaseLoader()} for more.
     * @param hostInterfaces the interfaces with which you can interact with the plugins.
     * @return the loaded plugins. You can use <code>instanceof</code> on each plugin if you want to know which
     * of the given host interfaces it implements, so you can later load it with {@link #loadPlugin(Class, String)}.
     */
    public List<IEffect> loadAllPlugins(List<Class<? extends IEffect>> hostInterfaces) throws IOException;

    /**
     * Load a single plugin whose class name is known from this bundle
     * @param type the interfaces with which you want to interact with the plugin
     * @param className the name of the plugin class
     * @return the plugin, bridged to the specified interface, or <code>null</code> if the plugin
     * cannot be bridged to the given interface or cannot be found
     */
    public <E extends IEffect> E loadPlugin(Class<E> type, String className);

    /**
     * Release the {@link ClassLoader} associated to this {@link IPluginBundle}. A new ClassLoader will
     * be created automatically again if {@link #loadAllPlugins(Class)} or {@link #loadPlugin(Class, String)}
     * is invoked.
     * <p>
     * If you do not keep references to the loaded {@link IEffect} instances, invoking this method
     * will allow both the plugin instances and the plugin classes to be garbage collected. It is possible
     * to invoke this method even if plugins are still loaded, as it only remove the reference to the
     * class loader, allowing it to be garbage collected when all loaded plugins are unloaded.
     * <p>
     * This method makes it possible to work with the plugins without having them constantly loaded in memory:
     * <ul>
     * <li>Load the available plugins (without using them) using {@link #loadAllPlugins(Class)}
     * <li>Copy the information from each plugin (such as {@link IEffect#getClassName()},
     * {@link IEffect#getInterface()}, {@link IEffect#getName()}) to create a plugin list without
     * keeping an explicit reference to the returned plugin instances.
     * <li>Use {@link #releaseLoader()} to make the plugins and plugin classes eligible for garbage collection
     * <li>Later re-instanciate any plugin for processing using {@link #loadPlugin(Class, String)}
     * <li>Use {@link #releaseLoader()} to make the plugin and plugin class eligible for garbage collection
     * </ul>
     * In all cases you can safely keep a reference to the {@link IPluginBundle}.
     * <p>
     * <b>Note about class garbage collection</b>: To allow plugin classes loaded with the bundle to be removed
     * from memory, the JVM must support <i>class garbage collection</i> (class GC). Not every JVM supports
     * class GC, and our experience is that even on JVM that support it, class GC rarely occurs (less frequently
     * than full GC, and not necessarily on a {@link System#gc()} call). Note that class GC is only necessary
     * to free the classes and associated static fields; instances are garbage collected as usual.
     * <p>
     * To work properly on JVM without class GC, this class will keep {@link WeakReference} to the {@link ClassLoader}s
     * and reuse them whenever they are still in memory (as long as the same file or URL is used), in order to
     * avoid new versions of the classes to be loaded. It is also recommended for plugin developers to avoid
     * static variables if possible.
     */
    public void releaseLoader();

}