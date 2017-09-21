package ch.tachyon.tunnel.engine.gui;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.param.*;


public class DoubleControl  extends JPanel {

    private final IParameter parameter;
    private final IProcessingInfo info;
    private final ScaleType scaleType;
    private final double minValue;
    private final double maxValue;
    private final int nbSteps;
    private JSlider slider;
    private JSpinner spinner;

    private double curValue;
    private boolean isAdjusting = false;


    public DoubleControl(IParameter parameter, IProcessingInfo info) {
        this.parameter = parameter;
        this.info = info;
        this.scaleType = parameter.getScaleType();
        Range range = parameter.getRange();
        this.minValue = (range == null ? Double.NEGATIVE_INFINITY : range.minValue());
        this.maxValue = (range == null ? Double.POSITIVE_INFINITY : range.maxValue());
        this.curValue = readValue();
        if (minValue > maxValue)
            throw new IllegalArgumentException("minValue (" + minValue + ") cannot be greater than maxValue (" + maxValue + ") for parameter " + parameter);
        if (curValue < minValue) {
            if ((float) curValue == (float) minValue)
                curValue = minValue;
            else
                throw new IllegalArgumentException("defaultValue (" + curValue + ") cannot be less than minValue (" + minValue + ") for parameter " + parameter);
        }
        if (curValue > maxValue) {
            if ((float) curValue == (float) maxValue)
                curValue = maxValue;
            else
                throw new IllegalArgumentException("defaultValue (" + curValue + ") cannot be greater than maxValue (" + maxValue + ") for parameter " + parameter);
        }
        Scale scale = parameter.getAnnotation(Scale.class);
        if (scale != null)
            nbSteps = scale.steps();
        else
            nbSteps = 10;
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        // Create slider
        if (!Double.isInfinite(minValue) && !Double.isInfinite(maxValue)) {
            this.slider = new JSlider(SwingConstants.HORIZONTAL, toSlider(minValue), toSlider(maxValue), toSlider(curValue));
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

        // Create number spinner
        this.spinner = new JSpinner();
        add(spinner);

        if (parameter.getDescription() != null) {
            String tooltip = ParameterGuiBuilder.formatMultiLineAsHtml(parameter.getDescription());
            spinner.setToolTipText(tooltip);
        }

        // Update spinner
        final double maxValueT = transformValue(maxValue);
        final double minValueT = transformValue(minValue);
        double curValueT = transformValue(curValue);
        double ticks = (maxValue - minValue) / (double) nbSteps; // based on untransformed values
        SpinnerModel model = new SpinnerNumberModel(curValueT, minValueT, maxValueT, ticks) {

            private static final long serialVersionUID = 3232386477406960692L;

            @Override
            public Object getNextValue() {
                double curValueT = (Double) super.getValue();
                double curValue = untransformValue(curValueT);
                double result = backScale(forwScale(curValue) + super.getStepSize().doubleValue());
                double resultT = transformValue(result);
                if (resultT < minValueT)
                    resultT = minValueT;
                else if (resultT > maxValueT)
                    resultT = maxValueT;
                return resultT;
            }

            @Override
            public Object getPreviousValue() {
                double curValueT = (Double) super.getValue();
                double curValue = untransformValue(curValueT);
                double result = backScale(forwScale(curValue) - super.getStepSize().doubleValue());
                double resultT = transformValue(result);
                if (resultT < minValueT)
                    resultT = minValueT;
                else if (resultT > maxValueT)
                    resultT = maxValueT;
                return resultT;
            }

        };
        if (!Double.isInfinite(minValue) && !Double.isInfinite(maxValue))
            spinner.setModel(model);
        else
            spinner.setModel(new SpinnerNumberModel(curValue, minValue, maxValue, 1.0));
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

    private double readValue() {
        Object data = parameter.getInternalValue();
        if (data instanceof Number)
            return ((Number) data).doubleValue();
        return 0.0;
    }

    private void writeValue(double value) {
        if (parameter.getType().equals(Double.class) || parameter.getType().equals(Double.TYPE))
            parameter.setInternalValue(value);
        else
            parameter.setInternalValue((float) value);
    }

    /**
     * Transform stored value into displayed value
     */
    private double transformValue(double value) {
        return parameter.getTransform().transform(value, new ProcessingInfoBridge(info));
    }

    /**
     * Transform displayed value into stored value
     */
    private double untransformValue(double value) {
        return parameter.getTransform().untransform(value, new ProcessingInfoBridge(info));
    }

    private boolean fireValue(double value) {
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
        if (fireValue(untransformValue(((Number) spinner.getValue()).doubleValue())))
            updateSlider();
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
    private double forwScale(double value) {
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
    private double backScale(double value) {
        double result = scaleType.backScale(value, minValue, maxValue);
        // Correct any roundoff
        if (result < minValue)
            result = minValue;
        else if (result > maxValue)
            result = maxValue;
        return result;
    }

    /**
     * Convert the stored value for the underlying parameter to the 0..1000 range, taking care of any {@link ScaleType}.
     */
    private int toSlider(double value) {
        value = forwScale(value);
        return (int) (1000.0 * (value - minValue) / (maxValue - minValue) + 0.5);
    }

    private double fromSlider(int value) {
        double res = (double) value * (maxValue - minValue) / 1000.0 + minValue;
        return backScale(res);
    }

}
