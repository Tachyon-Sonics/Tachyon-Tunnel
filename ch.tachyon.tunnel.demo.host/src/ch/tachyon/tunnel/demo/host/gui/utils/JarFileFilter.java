package ch.tachyon.tunnel.demo.host.gui.utils;

import java.io.*;

import javax.swing.filechooser.FileFilter;

/**
 * {@link FileFilter} accepting <tt>.jar</tt> files. Used to choose a plugin bundle
 */
public class JarFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;
        if (f.getName().toLowerCase().endsWith(".jar"))
            return true;
        return false;
    }

    @Override
    public String getDescription() {
        return "Plugin Bundles (*.jar)";
    }

}
