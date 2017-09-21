package ch.tachyon.tunnel.engine.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;

public class EffectDialog extends JDialog {

    private final IGuiListener listener;


    public EffectDialog(JFrame parent, IEffect effect, IProcessingInfo info, IGuiListener listener) {
        super(parent, effect.getName(), true);
        this.listener = listener;
        super.setLayout(new BorderLayout());
        EffectControlPanel controlPanel = new EffectControlPanel(effect, info);
        JPanel cp = new JPanel();
        cp.setLayout(new BorderLayout());
        cp.setBorder(new TitledBorder("Parameters"));
        cp.add(controlPanel, BorderLayout.CENTER);
        add(cp, BorderLayout.CENTER);
        add(getButtonsPanel(), BorderLayout.SOUTH);
        super.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                EffectDialog.this.listener.cancel();
            }

        });
    }

    private JPanel getButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 2));

        JPanel okPanel = new JPanel();
        okPanel.setLayout(new FlowLayout());
        panel.add(okPanel);
        Action okAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                listener.process();
            }
        };
        JButton okButton = new JButton(okAction);
        okButton.setText("Ok");
        okButton.setToolTipText("Apply the effect to the selection");
        okButton.setMnemonic('o');
        AwtTools.addKeyboardShortcut(okButton, KeyEvent.VK_O);
        okPanel.add(okButton);
        if (!System.getProperty("os.name").toLowerCase().equals("mac os x")
                || !System.getProperty("java.version").startsWith("1.5."))
            /*
             * Fix for Mac OS X 1.5 JVM: typing ENTER clicks on ok button before the
             * JSpinner have a chance to update their value
             */
            getRootPane().setDefaultButton(okButton);

        JPanel cancelPanel = new JPanel();
        cancelPanel.setLayout(new FlowLayout());
        panel.add(cancelPanel);
        Action cancelAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                listener.cancel();
            }
        };
        JButton cancelButton = new JButton(cancelAction);
        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Close this window without applying the effect");
        cancelButton.setMnemonic('c');
        AwtTools.addKeyboardShortcut(cancelButton, KeyEvent.VK_C);
        AwtTools.addKeyboardShortcut(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), false);
        cancelPanel.add(cancelButton);

        return panel;
    }

}
