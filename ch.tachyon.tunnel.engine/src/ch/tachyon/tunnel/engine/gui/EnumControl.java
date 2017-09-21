package ch.tachyon.tunnel.engine.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.param.*;

public class EnumControl extends JPanel {

    private final IParameter parameter;
    private JComboBox<Object> comboBox;
    private Object[] values;


    public EnumControl(IParameter parameter) {
        this.parameter = parameter;
        values = parameter.getType().getEnumConstants();
        Range range = parameter.getRange();
        if (range != null) {
            int min = Math.max(0, (int) range.minValue());
            int max = Math.min(values.length - 1, (int) range.maxValue());
            Object[] subRange = new Object[max - min + 1];
            System.arraycopy(values, min, subRange, 0, max - min + 1);
            values = subRange;
        }
        setLayout(new BorderLayout());
        comboBox = new JComboBox<Object>(values);
        add(comboBox, BorderLayout.CENTER);
        comboBox.setSelectedItem(parameter.getInternalValue());
        if (parameter.getDescription() != null) {
            String tooltip = ParameterGuiBuilder.formatMultiLineAsHtml(parameter.getDescription());
            comboBox.setToolTipText(tooltip);
        }
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                valueChanged();
            }
        });
        valueChanged();
    }

    private void valueChanged() {
        int index = comboBox.getSelectedIndex();
        if (index >= 0 && index < values.length)
            parameter.setInternalValue(values[index]);
    }

}
