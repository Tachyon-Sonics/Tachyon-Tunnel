package ch.tachyon.tunnel.demo.host.gui.utils;

import javax.swing.*;

/**
 * {@link JFileChooser} is very slow to load when used for the first time, and is usually one of the first user interaction.
 * If the very first user interaction in a Java application is slow, then Java must be slow, eh?
 * <p>
 * Let's try to debunk the myth...
 * <p>
 * This class attempts to speed it up (in a background thread) by instantiating a dummy chooser,
 * which should preload most of the classes and resources.
 */
public class FileChooserBooster implements Runnable {

    private static boolean started = false;
    private static final Object chooserLock = new Object();
    private static JFileChooser chooser = null;


    public void run() {
        try {
            String lafName = UIManager.getLookAndFeel().getName();
            if (lafName.contains("Substance")) // Fail early (see catch)
                throw new IllegalStateException();
            prepareFileChooser();
        } catch (Throwable ex) {
            // Some look and feel do not allow JFileChooser creation outside of EDT and throw crap
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    prepareFileChooser();
                }
            });
        }
    }

    private void prepareFileChooser() {
        // JFileChooser
        {
            try {
                // Pre-load icons used by the JFileChooser...
                // Quite OS specific, but does not harm...
                UIManager.getIcon("FileView.directoryIcon");
                UIManager.getIcon("FileView.fileIcon");
                UIManager.getIcon("FileView.computerIcon");
                UIManager.getIcon("FileView.hardDriveIcon");
                UIManager.getIcon("FileView.floppyDriveIcon");
                UIManager.getIcon("FileChooser.homeFolderIcon");
                UIManager.getIcon("FileChooser.newFolderIcon");
                UIManager.getIcon("FileChooser.upFolderIcon");
                UIManager.getIcon("FileChooser.detailsViewIcon");
                UIManager.getIcon("FileChooser.listViewIcon");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            JFileChooser chooser = new JFileChooser();
            chooser.validate();
            // Setting the current directory seems slow on XP. Do it now...
            chooser.setCurrentDirectory(chooser.getFileSystemView().getDefaultDirectory());
            // All the above stuff would be done again if we used a new JFileChooser instance...
            // Hence we provide a way to re-use the prepared version
            FileChooserBooster.chooser = chooser;
            synchronized (chooserLock) {
                chooserLock.notifyAll();
            }
        }
    }

    public static void start() {
        Thread t = new Thread(new FileChooserBooster());
        t.setName("Swing pre-compiler");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        started = true;
    }

    public static JFileChooser getFileChooser() {
        synchronized (chooserLock) {
            if (!started) {
                start();
                System.err.println("To really benefit from FileChooserBooster, you should invoke start as soon as your GUI is ready!");
            }
            while (chooser == null) {
                try {
                    chooserLock.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return chooser;
    }

}
