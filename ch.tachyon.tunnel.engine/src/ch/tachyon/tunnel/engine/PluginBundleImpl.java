package ch.tachyon.tunnel.engine;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.bridges.mthread.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * A collection of {@link IEffect plugins} in a .jar file.
 * <p>
 * This is the main class you use to load plugins dynamically from a .jar file.
 * <p>
 * To load a plugin statically (if it is present is the current class path), use {@link StaticLoader} instead.
 * <p>
 * Note: the plugins of a bundle are loaded with a special {@link URLClassLoader} implementation that
 * searches classes and resources on the given .jar file first, then among already loaded classes, and then
 * on the parent ClassLoader.
 * <p>
 * Furthermore, each {@link PluginBundleImpl} instance uses its own ClassLoader. This allows multiple
 * plugin bundles that use different versions of the same library to be used at the same time without conflict,
 * and also to use plugin bundles that use a different version of a library used by the host.
 * <p>
 * On the other hand, this may yield to a lot of loaded classes because several versions of the same library
 * might be loaded at the same time. Hence you may want to use {@link #releaseLoader()} whenever possible if
 * you are limited in terms of available memory.
 */
public class PluginBundleImpl implements IPluginBundle {

    private final URL url;
    private final boolean external;
    private final MtContext mtContext = new MtContext();
    private ClassLoader classLoader;
    private String bundleName;

    private static Map<String, WeakReference<ClassLoader>> classLoaderCache = new HashMap<String, WeakReference<ClassLoader>>();


    /**
     * Create a new plugin bundle using the given .jar file.
     * The plugins are not loaded yet.
     * @param jarFile the .jar file from which the plugins will be loaded. This can also be a directory of
     * .class files.
     * @param external <code>true</code> if the .jar file is not in the class path, <code>false</code> else.
     * <p>
     * <b>Warning</b>: if the plugin is in the class path, you must pass <code>false</code> to ensure the current
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
    public PluginBundleImpl(File jarFile, boolean external) {
        this(file2url(jarFile), external);
    }

    private static URL file2url(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (IOException ex) {
            // Not too bad. Canonicalization is only used to make the shit look better in the debugger
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Create a new plugin bundle using the given .jar file URL.
     * The plugins are not loaded yet.
     * @param jarFileUrl an URL to the .jar file from which the plugins will be loaded.
     * @param external <code>true</code> if the .jar file is not in the class path, <code>false</code> else.
     * See {@link Loader#loadPluginBundle(File, boolean)} for more information.
     */
    public PluginBundleImpl(URL jarFileUrl, boolean external) {
        this.url = jarFileUrl;
        this.external = external;
    }

    private ClassLoader classLoader() {
        if (classLoader == null) {
            // Look if not yet garbage collected
            WeakReference<ClassLoader> classLoaderRef = classLoaderCache.get(url.toExternalForm());
            if (classLoaderRef != null)
                classLoader = classLoaderRef.get();
            if (classLoader == null) {
                // Create a new one
                Debug.info("[New ClassLoader created]");
                classLoader = new ParentLastUrlClassLoader(new URL[] {url}, external);
                classLoaderRef = new WeakReference<ClassLoader>(classLoader);
                classLoaderCache.put(url.toExternalForm(), classLoaderRef);
            }
        }
        return classLoader;
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#getClassLoader(boolean)
     */
    public ClassLoader getClassLoader(boolean create) {
        if (create)
            return classLoader();
        return classLoader;
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#getMtContext()
     */
    public IMtContext getMtContext() {
        return mtContext;
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#getUrlString()
     */
    public String getUrlString() {
        return url.toExternalForm();
    }

    public String getName() {
        if (bundleName != null)
            return bundleName;
        ClassLoader cl = classLoader();
        InputStream stream = cl.getResourceAsStream("META-INF/MANIFEST.MF");
        if (stream == null)
            return null;
        try {
            Manifest manifest = new Manifest(stream);
            bundleName = manifest.getMainAttributes().getValue("Title");
            return bundleName;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public <E extends IEffect> List<E> loadAllPlugins(Class<E> hostInterface) throws IOException {
        List<Class<? extends IEffect>> singleTonIntf = new ArrayList<Class<? extends IEffect>>(1);
        singleTonIntf.add(hostInterface);
        List<IEffect> result0 = loadAllPlugins(singleTonIntf);
        for (IEffect effect : result0)
            assert hostInterface.isAssignableFrom(effect.getClass());
        @SuppressWarnings("unchecked")
        List<E> result = (List<E>) result0;
        return result;
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#loadAllPlugins(java.lang.Class)
     */
    public List<IEffect> loadAllPlugins(List<Class<? extends IEffect>> hostInterfaces) throws IOException {
        return PluginsImpl.loadAllPlugins(classLoader(), mtContext, hostInterfaces);
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#loadPlugin(java.lang.Class, java.lang.String)
     */
    public <E extends IEffect> E loadPlugin(Class<E> type, String className) {
        return PluginsImpl.loadPlugin(classLoader(), mtContext, type, className);
    }

    /* (non-Javadoc)
     * @see ch.tachyon.tunnel.host.IPluginBundle#releaseLoader()
     */
    public void releaseLoader() {
        classLoader = null;
    }

    @Override
    public String toString() {
        return "PluginBundle [url=" + url + "]";
    }

}
