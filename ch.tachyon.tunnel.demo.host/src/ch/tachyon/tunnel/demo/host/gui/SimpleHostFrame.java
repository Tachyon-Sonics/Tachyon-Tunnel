package ch.tachyon.tunnel.demo.host.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.demo.host.*;
import ch.tachyon.tunnel.demo.host.gui.utils.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

public class SimpleHostFrame extends JFrame {
    
    private ValueHolder<File> srcFileRef = new ValueHolder<File>();
    private ValueHolder<File> bundleFileRef = new ValueHolder<File>();
    private ValueHolder<IPluginBundle> bundleRef = new ValueHolder<IPluginBundle>();
    private ValueHolder<String> pluginClassRef = new ValueHolder<String>();
    private ValueHolder<File> dstFileRef = new ValueHolder<File>();
    
    
    static enum FileFilterType {
        OPEN_AUDIO,
        OPEN_BUNDLE,
        SAVE_AUDIO;
    }
    
    public SimpleHostFrame(final FormData form) {
        super("Tachyon Tunnel - Simple Demo Host Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set defaults
        srcFileRef.setValue(form.getSrcFile());
        dstFileRef.setValue(form.getDstFile());
        bundleFileRef.setValue(form.getBundleFile());

        // Bind values to the model. Ok, maybe here Java 8's lambdas might help?
        srcFileRef.addValueChangeListener(new Runnable() {
            public void run() {
                form.setSrcFile(srcFileRef.getValue());
            }
        });
        dstFileRef.addValueChangeListener(new Runnable() {
            public void run() {
                form.setDstFile(dstFileRef.getValue());
            }
        });
        bundleRef.addValueChangeListener(new Runnable() {
            public void run() {
                form.setBundle(bundleRef.getValue());
            }
        });
        pluginClassRef.addValueChangeListener(new Runnable() {
            public void run() {
                form.setEffectClassName(pluginClassRef.getValue());
            }
        });
        
        // Create UI
        setLayout(new BorderLayout());
        JPanel main = new JPanel();
        getContentPane().add(main, BorderLayout.CENTER);
        
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.insets = new Insets(6, 6, 6, 6);
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        gbc1.weightx = 1.0;
        main.setLayout(new TableLayout(2, gbc1));
        
        addFileSelection("Source Audio File", "Choose an existing audio file to transform.\nThis file won't be modified.", srcFileRef, form.getSrcAutoFile(), false, main, FileFilterType.OPEN_AUDIO);
        addFileSelection("Plugin Bundle", "Choose a plugin bundle (.jar file)", bundleFileRef, form.getBundleAutoFile(), false, main, FileFilterType.OPEN_BUNDLE);
        addPluginSelector("Audio Effect", "Choose a plugin from the bundle to transform the audio file", bundleRef, pluginClassRef, main, gbc1, form);
        
        addFileSelection("Save As (optional)", "Choose where to save the transformed file", dstFileRef, form.getDstAutoFile(), true, main, FileFilterType.SAVE_AUDIO);
        
        JPanel buttonsPanel = new JPanel();
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.weightx = 1.0;
        gbc2.insets = new Insets(8, 8, 8, 8);
        buttonsPanel.setLayout(new TableLayout(2, gbc2));
        
        final JButton btnTransform = new JButton("Transform...");
        btnTransform.setToolTipText("<html><body>Transform the source audio file using the selected plugin,<p />and optionally save the result to the selected 'Save As' file."
                + "<p /><p />A dialog will be opened to choose the effect parameters</body></html>");
        buttonsPanel.add(btnTransform);
        btnTransform.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                transform(btnTransform, form);
            }
        });
        
        JButton btnPlayResult = new JButton("Play Result");
        btnPlayResult.setToolTipText("You must Transform the audio file first");
        btnPlayResult.setEnabled(false);
        buttonsPanel.add(btnPlayResult);
        
        pack();
        getRootPane().setDefaultButton(btnTransform);
        
        // Load bundle now if a bundle file is specified
        if (bundleFileRef.getValue() != null && bundleFileRef.getValue().isFile()) {
            SwingUtilities.invokeLater(new Runnable() {
                
                public void run() {
                    loadBundle(form);
                }
            });
        }
        // Load bundle whenever bundle file is changed
        bundleFileRef.addValueChangeListener(new Runnable() {
            
            public void run() {
                loadBundle(form);
            }
        });
    }
    
    // UI Creation parts
    
    /**
     * Add two components to the target container:
     * <p>
     * The first component is a label with the given text.
     * <p>
     * The second component consists of a text field, with a small "..." button on its right.
     * The text field allows typing a file name while the button opens a file chooser.
     * @param text the text to display as a label
     * @param fileRef the {@link ValueHolder} that receive the user submitted value
     * @param target the container in which to add the components
     */
    private void addFileSelection(String text, String tooltip, final ValueHolder<File> fileRef, final File autoFile, final boolean saveMode, Container target,
            final FileFilterType fileFilterType) {
        JLabel label = new JLabel(text);
        label.setToolTipText(tooltip);
        target.add(label);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        target.add(panel);
        
        final JTextField textField = new JTextField();
        textField.setColumns(30);
        textField.setToolTipText(tooltip);
        File file = fileRef.getValue();
        if (file != null && (file.exists() || saveMode))
            textField.setText(file.getPath());
        panel.add(textField, BorderLayout.CENTER);
        textField.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                File file = new File(text);
                fileRef.setValue(file);
            }
        });
        
        final JButton btnChoose = new JButton("...");
        btnChoose.setToolTipText("Choose a file...");
        panel.add(btnChoose, BorderLayout.EAST);
        btnChoose.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                chooseFile(fileRef, autoFile, saveMode, textField, btnChoose, fileFilterType);
            }
        });
    }
    
    private void chooseFile(final ValueHolder<File> fileRef, final File autoFile, final boolean saveMode, final JTextField textField, final JButton btnChoose, FileFilterType fileFilterType) {
        JFileChooser chooser = FileChooserBooster.getFileChooser();
        chooser.setMultiSelectionEnabled(false);
        installFileFilters(chooser, fileFilterType);
        File curFile = fileRef.getValue();
        if (curFile != null && curFile.exists()) {
            chooser.setCurrentDirectory(curFile.getParentFile());
            chooser.setSelectedFile(curFile);
        } else {
            if (autoFile.isDirectory())
                chooser.setCurrentDirectory(autoFile);
            else {
                chooser.setCurrentDirectory(autoFile.getParentFile());
                chooser.setSelectedFile(autoFile);
            }
        }
        while (true) { // Loop until choice is approved
            int result;
            if (saveMode)
                result = chooser.showSaveDialog(btnChoose);
            else
                result = chooser.showOpenDialog(btnChoose);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosenFile = chooser.getSelectedFile();
                if (saveMode && chosenFile.exists()) {
                    // Ask for confirmation - replacing existing file
                    int confirm = JOptionPane.showConfirmDialog(btnChoose, "File \"" + chosenFile.getName() + "\" already exists. Replace it?", "Confirm replace", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (confirm == JOptionPane.NO_OPTION)
                        continue; // Ask again another file
                    if (confirm != JOptionPane.YES_OPTION)
                        break; // Cancel
                    // Confirmed. Set file and leave
                }
                fileRef.setValue(chosenFile);
                textField.setText(chosenFile.getPath());
            }
            // Canceled, not overwriting existing file, or approved: we are done
            break;
        }
    }
    
    private void installFileFilters(JFileChooser chooser, FileFilterType fileFilterType) {
        // Remove previous filters
        for (FileFilter filter : chooser.getChoosableFileFilters())
            chooser.removeChoosableFileFilter(filter);
        // Allow "all files"
        chooser.setAcceptAllFileFilterUsed(true);
        // Add new filters
        switch (fileFilterType) {
            case OPEN_AUDIO: {
                chooser.addChoosableFileFilter(AudioFileFilter.readInstance());
                for (AudioFileFilter filter : AudioFileFilter.individualInstances(false))
                    chooser.addChoosableFileFilter(filter);
                chooser.setFileFilter(AudioFileFilter.readInstance());
                break;
            }
            case SAVE_AUDIO: {
                for (AudioFileFilter filter : AudioFileFilter.individualInstances(true))
                    chooser.addChoosableFileFilter(filter);
                break;
            }
            case OPEN_BUNDLE: {
                chooser.addChoosableFileFilter(new JarFileFilter());
                break;
            }
        }
    }

    private void addPluginSelector(String text, String tooltip, final ValueHolder<IPluginBundle> bundleRef, ValueHolder<String> pluginClassRef, Container target, GridBagConstraints sgbc,
            final FormData form) {
        JLabel label = new JLabel(text);
        label.setToolTipText(tooltip);
        target.add(label);

        final JComboBox<EffectDescriptor> selector = new JComboBox<EffectDescriptor>();
        target.add(selector, sgbc);
        
        selector.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                effectUpdated(form, selector);
            }
        });
        
        if (bundleRef.getValue() != null) {
            fillChooserWithAvailablePlugins(form, selector);
        } else {
            selector.setEnabled(false);
            selector.setToolTipText("Please choosen a plugin bundle first");
        }
        
        bundleRef.addValueChangeListener(new Runnable() {
            
            public void run() {
                if (bundleRef.getValue() != null)
                    fillChooserWithAvailablePlugins(form, selector);
            }
        });
    }
    
    // Actions
    
    /**
     * Load the selected plugin bundle.
     * <p>
     * Invoked when a plugin file is specified
     */
    private void loadBundle(final FormData form) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setEnabled(false);
        form.setBundleFile(bundleFileRef.getValue());
        try {
            loadBundleImpl(form);
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            setEnabled(true);
        }
    }
    
    private void loadBundleImpl(FormData form) {
        File bundleFile = bundleFileRef.getValue();
        if (!bundleFile.isFile()) {
            JOptionPane.showMessageDialog(this, "File not found: " + bundleFile, "Load Bundle", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            IPluginBundle bundle = Controller.loadPluginBundle(form.getBundleFile());
            bundleRef.setValue(bundle);
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cannot load plugin bundle: " + ex.getMessage(), "Load Bundle", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Fill the plugin chooser with the plugin available in the bundle.
     * <p>
     * Invoked when a new plugin bundle is specified
     * @param selector the plugin selector to fill
     */
    private void fillChooserWithAvailablePlugins(FormData form, JComboBox<EffectDescriptor> selector) {
        selector.setEnabled(false);
        selector.setToolTipText("Please choosen a plugin bundle first");

        List<IPushEffect> effects = null;
        try {
            effects = Controller.loadAllPlugins(form.getBundle());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(selector, "Cannot load plugins from " + form.getBundle().getUrlString() + ": " + ex.getMessage(), "Error Loading Plugin Bundle", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (effects == null || effects.isEmpty()) {
            selector.setModel(new DefaultComboBoxModel<EffectDescriptor>(new EffectDescriptor[0]));
            return;
        }
        
        // Populate
        selector.setEnabled(true);
        selector.setToolTipText("Choose a plugin from the bundle");
        
        /*
         * We do not want to keep all effect instances in memory. It can take a huge amount of space
         * (although plugin makers are not encouraged in allocating resources before processing).
         * Hence we just build a simple descriptor for each effect. Then we can release all effects
         * and the classloader. We can still instanciate the effect later unsing the bundle.
         */
        EffectDescriptor[] descriptors = Controller.gatherPluginDescriptions(effects);
        selector.setModel(new DefaultComboBoxModel<EffectDescriptor>(descriptors));
        Controller.releaseBundleLoader(form.getBundle());

        // Select the first plugin automatically
        effectUpdated(form, selector);
    }
    
    private void effectUpdated(FormData form, JComboBox<EffectDescriptor> selector) {
        EffectDescriptor descr = (EffectDescriptor) selector.getSelectedItem();
        pluginClassRef.setValue(descr.getClassName());
        form.setEffectParameters(null);
    }
    
    /**
     * Launch the transformation. Invoked when the "Transform" button is clicked.
     * <p>
     * The work is delegated to the {@link Controller}, which uses {@link ApplyPlugin}
     */
    private void transform(JButton btnTransform, final FormData form) {
        if (srcFileRef.getValue() == null || !srcFileRef.getValue().isFile()) {
            JOptionPane.showMessageDialog(btnTransform, "Please choose a source audio file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (bundleFileRef.getValue() == null || !bundleFileRef.getValue().isFile()) {
            JOptionPane.showMessageDialog(btnTransform, "Please choose a plugin bundle", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Open plugin setup dialog and store parameters in a serialized, compact way
        // These could be stored in a file to implement presets for instance
        byte[] storedParameters = Controller.openPluginDialog(this, form.getSrcFile(), form.getBundle(), form.getEffectClassName(), form.getEffectParameters());
        if (storedParameters == null) {
            System.out.println("User cancelled");
            return;
        }
        form.setEffectParameters(storedParameters);
        
        // Create a progress dialog
        String title = "Applying " + form.getEffectClassName();
        final ProgressDialog progressDialog = new ProgressDialog(this, title);
        progressDialog.setLocationRelativeTo(this);
        handleProcessingStart(progressDialog);
        
        // Start processing using a new thread
        Thread processingThread = new Thread("Processing Thread") {
            @Override
            public void run() {
                try {
                    /*********************************
                     * Processing takes place here:  *
                     *********************************/
                    Controller.applyTransform(form.getSrcFile(), form.getBundle(), form.getEffectClassName(), form.getEffectParameters(), form.getDstFile(), progressDialog);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        
                        public void run() {
                            handleProcessingStop(progressDialog);
                        }
                    });
                }
            }
        };
        processingThread.start();
    }

    private void handleProcessingStart(final ProgressDialog progressDialog) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressDialog.setVisible(true);
        setEnabled(false);
    }
    
    private void handleProcessingStop(final ProgressDialog progressDialog) {
        setCursor(null);
        setEnabled(true);

        progressDialog.setVisible(false);
        progressDialog.dispose();
    }
    
}
