package ch.tachyon.tunnel.engine.gui;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.param.*;


public class LongControl extends JPanel {

    private final IParameter parameter;
    private final IProcessingInfo info;
    private final ScaleType scaleType;
    private final long minValue;
    private final long maxValue;
    private final int sliderMin;
    private final int sliderMax;
    private final boolean identityMapped;
    private JSlider slider;
    private JSpinner spinner;

    private long curValue;
    private boolean isAdjusting = false;


    public LongControl(IParameter parameter, IProcessingInfo info) {
        this.parameter = parameter;
        this.info = info;
        this.scaleType = parameter.getScaleType();
        Range range = parameter.getRange();
        this.minValue = (range == null ? getMinFor(parameter.getType()) : (long) range.minValue());
        this.maxValue = (range == null ? getMaxFor(parameter.getType()) : (long) range.maxValue());
        this.curValue = readValue();
        if (minValue > Short.MIN_VALUE && maxValue < Short.MAX_VALUE && scaleType.equals(ScaleType.LINEAR)) {
            sliderMin = (int) minValue;
            sliderMax = (int) maxValue;
            identityMapped = true;
        } else {
            sliderMin = 0;
            sliderMax = 1000;
            identityMapped = false;
        }
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        if (minValue > Integer.MIN_VALUE && maxValue < Integer.MAX_VALUE) {
            this.slider = new JSlider(SwingConstants.HORIZONTAL, toSlider(minValue), toSlider(maxValue),
                    toSlider(curValue));
            add(slider);
            if (parameter.getDescription() != null) {
                String tooltip = ParameterGuiBuilder.formatMultiLineAsHtml(parameter.getDescription());
                slider.setToolTipText(tooltip);
            }
            // Set slider bounds
            slider.setMinimum(toSlider(minValue));
            slider.setMaximum(toSlider(maxValue));
            slider.setValue(toSlider(curValue));
            slider.setPaintLabels(false);
            slider.setPaintTicks(identityMapped);
            if (identityMapped) {
                slider.setMinorTickSpacing(1);
                if (sliderMax - sliderMin >= 30)
                    slider.setMajorTickSpacing(10);
            }
            slider.setSnapToTicks(identityMapped);
            /*
             *  Ubuntu hack: Ubuntu always display the current value (as integer!).
             *  We can remove it by setting the font to a size of zero. Use
             *  try catch just in case...
             */
            if (UIManager.getLookAndFeel().getName().contains("GTK")) {
                try {
                    slider.setFont(getFont().deriveFont(0.0f));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            slider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    if (isAdjusting)
                        return;
                    isAdjusting = true;
                    sliderUpdated();
                    isAdjusting = false;
                }

            });
            slider.setFocusable(false);
        }

        this.spinner = new JSpinner();
        add(spinner);

        if (parameter.getDescription() != null) {
            String tooltip = ParameterGuiBuilder.formatMultiLineAsHtml(parameter.getDescription());
            spinner.setToolTipText(tooltip);
        }

        // Update spinner
        final long maxValueT = transformValue(maxValue);
        final long minValueT = transformValue(minValue);
        long curValueT = transformValue(curValue);
        SpinnerModel model = new SpinnerNumberModel(curValueT, minValueT, maxValueT, 1) {

            @Override
            public Object getNextValue() {
                long curValueT = ((Number) spinner.getValue()).longValue();
                long curValue = untransformValue(curValueT);
                long newValue = Math.min(curValue + 1, maxValue);
                long newValueT = transformValue(newValue);
                return newValueT;
            }

            @Override
            public Object getPreviousValue() {
                long curValueT = ((Number) spinner.getValue()).longValue();
                long curValue = untransformValue(curValueT);
                long newValue = Math.max(curValue - 1, minValue);
                long newValueT = transformValue(newValue);
                return newValueT;
            }

        };
        spinner.setModel(model);
        final JFormattedTextField textField = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        int columns = textField.getColumns();
        if (columns < 5)
            textField.setColumns(5);
        else if (columns > 20)
            textField.setColumns(20);
        textField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        textField.selectAll();
                    }
                });
            }

        });

        // Add listeners
        spinner.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (isAdjusting)
                    return;
                isAdjusting = true;
                spinnerUpdated();
                isAdjusting = false;
            }

        });
    }

    private static long getMinFor(Class<?> type) {
        if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            return Byte.MIN_VALUE;
        else if (type.equals(Short.class) || type.equals(Short.TYPE))
            return Short.MIN_VALUE;
        else if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            return Integer.MIN_VALUE;
        else if (type.equals(Long.class) || type.equals(Long.TYPE))
            return Long.MIN_VALUE;
        throw new IllegalArgumentException();
    }

    private static long getMaxFor(Class<?> type) {
        if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            return Byte.MAX_VALUE;
        else if (type.equals(Short.class) || type.equals(Short.TYPE))
            return Short.MAX_VALUE;
        else if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            return Integer.MAX_VALUE;
        else if (type.equals(Long.class) || type.equals(Long.TYPE))
            return Long.MAX_VALUE;
        throw new IllegalArgumentException();
    }

    private long readValue() {
        Object data = parameter.getInternalValue();
        if (data instanceof Number)
            return ((Number) data).longValue();
        return 0;
    }

    private void writeValue(long value) {
        Class<?> type = parameter.getType();
        if (type.equals(Long.class) || type.equals(Long.TYPE))
            parameter.setInternalValue(value);
        else if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            parameter.setInternalValue((int) value);
        else if (type.equals(Short.class) || type.equals(Short.TYPE))
            parameter.setInternalValue((short) value);
        else if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            parameter.setInternalValue((byte) value);
    }

    /**
     * Transform stored value into displayed value
     */
    private long transformValue(long value) {
        return parameter.getTransform().transform(value, new ProcessingInfoBridge(info));
    }

    /**
     * Transform displayed value into stored value
     */
    private long untransformValue(long value) {
        return parameter.getTransform().untransform(value, new ProcessingInfoBridge(info));
    }

    private boolean fireValue(long value) {
        value = Math.max(value, minValue);
        value = Math.min(value, maxValue);
        if (this.curValue != value) {
            this.curValue = value;
            writeValue(value);
            return true;
        }
        return false;
    }

    private void sliderUpdated() {
        if (fireValue(fromSlider(slider.getValue())))
            updateSpinner();
    }

    private void spinnerUpdated() {
        if (fireValue(untransformValue(((Number) spinner.getValue()).longValue())))
            updateSlider();
        // Re-update in case of rounding of illegal value:
        updateSpinner();
    }

    private void updateSlider() {
        if (slider != null)
            slider.setValue(toSlider(curValue));
    }

    private void updateSpinner() {
        spinner.setValue(transformValue(curValue));
    }

    /**
     * Convert actual value to slider value
     */
    private double forwScale(long value) {
        double result = scaleType.forwScale(value, minValue, maxValue);
        // Correct any roundoff
        if (result < minValue)
            result = minValue;
        else if (result > maxValue)
            result = maxValue;
        return result;
    }

    /**
     * Convert slider value to actual value
     */
    private long backScale(double value) {
        double temp = scaleType.backScale(value, minValue, maxValue);
        long result = (long) Math.round(temp);
        // Correct any roundoff
        if (result < minValue)
            result = minValue;
        else if (result > maxValue)
            result = maxValue;
        return result;
    }

    private int toSlider(long value) {
        double temp = forwScale(value);
        double sliderRange = (sliderMax - sliderMin);
        double valueRange = (maxValue - minValue);
        double valueOffset = (temp - minValue);
        return sliderMin + (int) Math.round((sliderRange * valueOffset) / valueRange);
    }

    private long fromSlider(int value) {
        double sliderRange = (sliderMax - sliderMin);
        double valueRange = (maxValue - minValue);
        double valueOffset = (value - minValue);
        double res = (valueOffset * valueRange ) / sliderRange + minValue;
        return backScale(res);
    }

}
