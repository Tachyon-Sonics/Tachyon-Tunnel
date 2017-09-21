package ch.tachyon.tunnel.demo.host.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import ch.tachyon.tunnel.demo.host.gui.utils.*;
import ch.tachyon.tunnel.utils.*;


public class ProgressDialog extends JDialog implements IProgressMonitor {
    
    private int lastPercent = -1;
    private JProgressBar progress;
    private volatile boolean aborted = false;
    
    
    public ProgressDialog(JFrame parent, String title) {
        super(parent, title, false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        GridBagConstraints gdc = new GridBagConstraints();
        gdc.insets = new Insets(6, 6, 6, 6);
        gdc.fill = GridBagConstraints.BOTH;
        gdc.weightx = 1.0;
        setLayout(new TableLayout(2, gdc));
        
        progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progress.setIndeterminate(true);
        add(progress);
        
        JButton btnAbort = new JButton("Abort");
        btnAbort.setToolTipText("Abort the processing");
        gdc.weightx = 0.0;
        add(btnAbort, gdc);
        btnAbort.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                aborted = true;
            }
        });
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(ProgressDialog.this, "Abort current processing?", "Abort", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION)
                    aborted = true;
            }
        });
        
        pack();
    }

    public void setProgress(double current, double total) throws OperationAbortedException {
        if (aborted)
            throw new OperationAbortedException(true);
        
        final int percent = (int) (current * 100.0 / total + 0.5);
        if (percent != lastPercent) {
            lastPercent = percent;
            SwingUtilities.invokeLater(new Runnable() {
                
                public void run() {
                    progress.setIndeterminate(false);
                    progress.setValue(percent);
                }
            });
        }

    }

}
