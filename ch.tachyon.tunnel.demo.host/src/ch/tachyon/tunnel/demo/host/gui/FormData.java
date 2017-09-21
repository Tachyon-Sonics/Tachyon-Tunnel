package ch.tachyon.tunnel.demo.host.gui;

import java.io.*;
import java.util.prefs.*;

import javax.swing.filechooser.*;

import ch.tachyon.tunnel.host.engine.*;

/**
 * Holds data edited by the user
 */
public class FormData {
    
    private File srcFile;
    private File dstFile;
    private File bundleFile;
    private IPluginBundle bundle;
    private String effectClassName;
    private byte[] effectParameters;

    private File srcAutoFile;
    private File dstAutoFile;
    private File bundleAutoFile;
    
    
    public FormData() {
        setupDefaults();
        loadFromPreferences();
    }

    public File getSrcFile() {
        return srcFile;
    }

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
        storeToPreferences();
    }

    public File getDstFile() {
        return dstFile;
    }

    public void setDstFile(File dstFile) {
        this.dstFile = dstFile;
        storeToPreferences();
    }

    public File getBundleFile() {
        return bundleFile;
    }

    public void setBundleFile(File bundleFile) {
        this.bundleFile = bundleFile;
        storeToPreferences();
    }

    public IPluginBundle getBundle() {
        return bundle;
    }

    public void setBundle(IPluginBundle bundle) {
        this.bundle = bundle;
    }

    public String getEffectClassName() {
        return effectClassName;
    }

    public void setEffectClassName(String effectClassName) {
        this.effectClassName = effectClassName;
    }
    
    public byte[] getEffectParameters() {
        return effectParameters;
    }
    
    public void setEffectParameters(byte[] effectParameters) {
        this.effectParameters = effectParameters;
    }
    
    public File getSrcAutoFile() {
        return srcAutoFile;
    }

    public File getDstAutoFile() {
        return dstAutoFile;
    }
    
    public File getBundleAutoFile() {
        return bundleAutoFile;
    }

    private void setupDefaults() {
        srcAutoFile = FileSystemView.getFileSystemView().getDefaultDirectory();
        dstAutoFile = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "Result.wav");
        bundleAutoFile = new File(new File("").getAbsoluteFile(), "Demo-Plugins.jar");
    }

    private void loadFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(FormData.class);
        String srcPath = prefs.get("srcFile", null);
        if (srcPath != null)
            srcFile = new File(srcPath);
        String dstPath = prefs.get("dstFile", null);
        if (dstPath != null)
            dstFile = new File(dstPath);
        String bundlePath = prefs.get("bundleFile", null);
        if (bundlePath != null)
            bundleFile = new File(bundlePath);
    }
    
    private void storeToPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(FormData.class);
        if (srcFile != null)
            prefs.put("srcFile", srcFile.getPath());
        if (dstFile != null)
            prefs.put("dstFile", dstFile.getPath());
        if (bundleFile!= null)
            prefs.put("bundleFile", bundleFile.getPath());
    }

}
