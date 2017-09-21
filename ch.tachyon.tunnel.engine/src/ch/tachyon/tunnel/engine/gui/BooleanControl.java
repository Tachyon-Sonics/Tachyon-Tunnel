package ch.tachyon.tunnel.engine.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import ch.tachyon.tunnel.host.effect.*;

public class BooleanControl extends JPanel {

    private final IParameter parameter;
    private JCheckBox checkBox;


    public BooleanControl(IParameter parameter) {
        this.parameter = parameter;
        setLayout(new BorderLayout());
        checkBox = new JCheckBox();
        checkBox.setSelected(readValue());
        add(checkBox, BorderLayout.CENTER);

        if (parameter.getDescription() != null) {
            String tooltip = ParameterGuiBuilder.formatMultiLineAsHtml(parameter.getDescription());
            checkBox.setToolTipText(tooltip);
        }

        checkBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                checkBoxClicked();
            }
        });
    }

    private void checkBoxClicked() {
        writeValue(checkBox.isSelected());
    }

    public boolean readValue() {
        Object value = parameter.getInternalValue();
        return Boolean.TRUE.equals(value);
    }

    public void writeValue(boolean value) {
        parameter.setInternalValue(value);
    }
}
