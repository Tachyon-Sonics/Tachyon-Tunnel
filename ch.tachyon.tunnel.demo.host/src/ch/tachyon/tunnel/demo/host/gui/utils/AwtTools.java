package ch.tachyon.tunnel.demo.host.gui.utils;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.UIManager.*;


public class AwtTools {

    private AwtTools() {

    }

    public static void initLookAndFeel(String appName) {
        // Some Apple-specific stuff:
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            // Patch for an utterly stupid menu bug; see https://netbeans.org/bugzilla/show_bug.cgi?id=198639
            try {
                Class<?> xwm = Class.forName("sun.awt.X11.XWM");
                Field awt_wmgr = xwm.getDeclaredField("awt_wmgr");
                awt_wmgr.setAccessible(true);
                Field metacity_wm = xwm.getDeclaredField("METACITY_WM");
                metacity_wm.setAccessible(true);
                awt_wmgr.set(null, metacity_wm.get(null));
                awt_wmgr.setAccessible(false);
                metacity_wm.setAccessible(false);
            }
            catch (Throwable x) {
                x.printStackTrace();
            }
            /*
             * GTK always display the current value (as integer!) over sliders.
             * This is almost always useless and stupid. Disable this behavior globally:
             */
            UIManager.getDefaults().put("Slider.paintValue", false);
        }

        // Switch to system look and feel, unless a laf is specified explicitely:
        if (System.getProperty("swing.defaultlaf") == null) {
            if (!System.getProperty("java.version").startsWith("1.5") && osName.contains("linux")) {
                /*
                 * Do not knwo why, on Linux, GTK is not always chosen. Try to install it manually
                 */
                for (LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
                    if (lafInfo.getName().toLowerCase().startsWith("gtk")) {
                        try {
                            setLookAndFeel(lafInfo.getClassName());
                            return; // Success
                        } catch (Throwable t) {
                            // Ignore. We will try the standard way below
                        }
                    }
                }
            }
            try {
                String name = UIManager.getSystemLookAndFeelClassName();
                setLookAndFeel(name);
            } catch (Throwable ex) { // OS/2 may throw NoClassDefFoundError (!)
                ex.printStackTrace();
            }
        }
        
        // Disable "rename" and "delete" in file chooser:
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    }

    private static void setLookAndFeel(final String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        if (!SwingUtilities.isEventDispatchThread()) { // Fix for Mac OS X: always use EDT
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        try {
                            setLookAndFeel(name);
                        } catch (Throwable ex) { // OS/2 may throw NoClassDefFoundError (!)
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        UIManager.setLookAndFeel(name);
    }

    public static void centerWindowOnScreen(Window window) {
        GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point center = gEnv.getCenterPoint();
        Dimension wDim = window.getSize();
        int x = center.x - wDim.width / 2;
        int y = center.y - wDim.height / 2;
        window.setLocation(x, y);
    }

}
