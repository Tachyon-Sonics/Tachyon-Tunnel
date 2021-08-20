package ch.tachyon.tunnel.engine.utils;

import java.io.*;
import java.net.*;
import java.util.*;

import ch.tachyon.tunnel.common.utils.*;

/**
 * A modified {@link URLClassLoader} that can be configured to load classes from the given
 * URL before using the parent {@link ClassLoader} (parent-last).
 * <p>
 * This class loader is used to load plugins from .jar file.
 */
public class ParentLastUrlClassLoader extends ClassLoader implements Closeable {

    private final ChildUrlClassLoader childFirstClassLoader;
    private final boolean parentLast;

    /**
     * This wrapper class allows me to call findClass on a classloader
     */
    private static class FindClassClassLoader extends ClassLoader {
        public FindClassClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }

        @Override
        public String findLibrary(String libname) {
            return super.findLibrary(libname);
        }

    }

    /**
     * This class delegates (child then parent) for the findClass method for a
     * URLClassLoader. We need this because findClass is protected in
     * URLClassLoader
     */
    private static class ChildUrlClassLoader extends URLClassLoader {
        private final boolean parentLast;
        private final FindClassClassLoader realParent;

        public ChildUrlClassLoader(URL[] urls, FindClassClassLoader realParent, boolean parentLast) {
            super(urls, null);
            this.realParent = realParent;
            this.parentLast = parentLast;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            // Check if we have already loaded the class:
            Class<?> result = super.findLoadedClass(name);
            if (result != null)
                return result;
            try {
                // first try to use the URLClassLoader findClass
                if (parentLast)
                    return super.findClass(name);
            } catch (ClassNotFoundException e) {
                // if that fails, we ask our real parent classloader to load the
                // class (we give up)
            }
            return realParent.loadClass(name);
        }

        @Override
        protected String findLibrary(String libname) {
            String result = null;
            if (parentLast)
                result = super.findLibrary(libname);
            if (result != null)
                return result;
            return realParent.findLibrary(libname);
        }

        public void dispose() {
            // Nice trick to use the close() method on Java >= 1.7:
            if (this instanceof Closeable) {
                try {
                    ((Closeable) this).close();
                } catch (IOException ex) {
                    Debug.error(ex);
                }
            }
        }

    }

    public ParentLastUrlClassLoader(URL[] urls, boolean parentLast) {
        super(Thread.currentThread().getContextClassLoader());
        childFirstClassLoader = new ChildUrlClassLoader(urls, new FindClassClassLoader(this.getParent()),
                parentLast);
        this.parentLast = parentLast;
    }

    // Loading

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!parentLast) {
            try {
                // Try parent first
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException ex) {
                // Not found, continue with child
            }
        }
        // Try with child-first
        return childFirstClassLoader.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        URL result = null;
        if (!parentLast) {
            // Try parent first
            result = super.getResource(name);
            if (result != null)
                return result;
        }
        // Try with child first
        if (parentLast)
            result = childFirstClassLoader.getResource(name);
        return result;
    }

    /**
     * Get resources in this classloader's jar file <i>without</i> looking for
     * resource in the parent classloader.
     * <p>
     * This differs from the {@link URLClassLoader}'s behavior, but is relevant in the
     * context of loading plugin bundles.
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return childFirstClassLoader.getResources(name);

//        // The following would return all resources from this class loader and the parent:
//        Collection<URL> result = new LinkedHashSet<URL>();
//        Enumeration<URL> childResources = childClassLoader.getResources(name);
//        while (childResources != null && childResources.hasMoreElements())
//            result.add(childResources.nextElement());
//        Enumeration<URL> parentResources = super.getResources(name);
//        while (parentResources != null && parentResources.hasMoreElements())
//            result.add(parentResources.nextElement());
//        return Collections.enumeration(result);
    }

    @Override
    protected String findLibrary(String libname) {
        String result = null;
        if (!parentLast)
            result = super.findLibrary(libname);
        if (result != null)
            return result;
        return childFirstClassLoader.findLibrary(libname);
    }

    public void close() {
        childFirstClassLoader.dispose();
    }

}
