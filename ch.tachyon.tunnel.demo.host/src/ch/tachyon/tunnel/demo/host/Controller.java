package ch.tachyon.tunnel.demo.host;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.sound.sampled.*;
import javax.swing.*;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

/**
 * This class encapsulates the different operations that are usually performed by a host to apply
 * a plugin on a file
 *
 */
public class Controller {

    /**
     * Load a plugin bundle for a .jar file
     * @param bundleFile the .jar file
     * @return the plugin bundle
     */
    public static IPluginBundle loadPluginBundle(File bundleFile) {
        return Loader.loadPluginBundle(bundleFile, true);
    }
    
    /**
     * Load all effects that are available in the given bundle
     */
    public static List<IPushEffect> loadAllPlugins(IPluginBundle bundle) throws IOException {
        return bundle.loadAllPlugins(IPushEffect.class);
    }
    
    /**
     * Extract plugins name and description from the list of loaded plugins.
     * <p>
     * Extracting that information allows us to present the list of available plugins to the user,
     * while releasing then (see {@link #releaseBundleLoader(IPluginBundle)}).
     */
    public static EffectDescriptor[] gatherPluginDescriptions(List<IPushEffect> effects) {
        EffectDescriptor[] descriptors = new EffectDescriptor[effects.size()];
        for (int i = 0; i < effects.size(); i++) {
            descriptors[i] = new EffectDescriptor(effects.get(i));
        }
        return descriptors;
    }
    
    /**
     * Release the bundle, so that plugin classes and the classloader can be garbage collected
     */
    public static void releaseBundleLoader(IPluginBundle bundle) {
        bundle.releaseLoader();
    }
    
    /**
     * Load the plugin, open its dialog, and then store the parameter values in a byte[] array.
     * <p>
     * Return <tt>null</tt> if the user clicks on cancel
     * @param parent the parent frame
     * @param inputFile the file to transform. It is required to retrieve the sample rate and other stuff that the plugin might require to adjust the parameters
     * @param bundle the plugin's bundle
     * @param effectClassName the plugin class name - comming from {@link EffectDescriptor#getClassName()}
     * @param storedParameters parameters stored from a previous call of this method, or null.
     * @return the parameter values, serialized, or null if the user clicks on cancel
     */
    public static byte[] openPluginDialog(JFrame parent, File inputFile, IPluginBundle bundle, String effectClassName, byte[] storedParameters) {
        try {
            AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFile, true);
            float sampleRate = inputFormat.getFormat().getSampleRate();
            int inputLength = inputFormat.getFrameLength();
            int nbChannels = inputFormat.getFormat().getChannels();
            
            IProcessingInfo info = new ProcessingInfo(nbChannels, sampleRate, 16, inputLength, ApplyPlugin.CHUNK_LENGTH, ApplyPlugin.CHUNK_LENGTH);
            
            final IPushEffect plugin = bundle.loadPlugin(IPushEffect.class, effectClassName);
            plugin.load(info);
            
            // Restore previous parameters, if any
            if (storedParameters != null) {
                try {
                    Storage.loadParameterValues(new ByteArrayInputStream(storedParameters), bundle, plugin.getParameters());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            final AtomicBoolean approved = new AtomicBoolean(false);
            plugin.showGui(parent, new GuiAdapter() {
                
                @Override
                public void process() {
                    approved.set(true);
                    /*
                     * Hide the plugin's window. Note that we *could* start the processing without closing
                     * the window, however if we do we must do it in a new/other thread because the window is
                     * modal and the current thread won't resume until the window is closed.
                     */
                    plugin.hideGui();
                }
                
                @Override
                public void cancel() {
                    approved.set(false);
                    plugin.hideGui();
                }
            });
            if (!approved.get())
                return null;
            
            ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
            try {
                Storage.storeParameterValues(plugin.getParameters(), memoryStream);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            
            plugin.unload();
            bundle.releaseLoader();
            
            return memoryStream.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Apply a plugin on a file. The work is delegated to the {@link ApplyPlugin} class.
     * @param srcFile the input audio file to transform
     * @param bundle the plugin's bundle
     * @param effectClassName the plugin class name - comming from {@link EffectDescriptor#getClassName()}
     * @param storedParameters the parameter values, serialized
     * @param dstFile the output audio file
     */
    public static void applyTransform(File srcFile, IPluginBundle bundle, String effectClassName, byte[] storedParameters, File dstFile, IProgressMonitor progress) {
        if (srcFile == null || bundle == null || effectClassName == null)
            throw new NullPointerException();
        
        System.out.println("Transforming \"" + srcFile + "\" to \"" + dstFile + "\" using " + effectClassName + " from " + bundle.getUrlString());
        try {
            ApplyPlugin.applyPlugin(srcFile, bundle, effectClassName, storedParameters, dstFile, progress);
            System.out.println("Successful!");
        } catch (OperationAbortedException ex) {
            System.out.println("Aborted by user");
        } catch (Exception ex) {
            ex.printStackTrace(); 
        }
    }
    

}
