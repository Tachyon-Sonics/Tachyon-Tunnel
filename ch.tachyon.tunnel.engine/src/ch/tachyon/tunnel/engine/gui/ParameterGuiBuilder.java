package ch.tachyon.tunnel.engine.gui;

import javax.swing.*;

import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;


public class ParameterGuiBuilder {

    public JComponent[] buildGuiFor(IParameter parameter, IProcessingInfo info) {
        JLabel label = new JLabel(parameter.getDisplayName());
        if (parameter.getDescription() != null)
            label.setToolTipText(formatMultiLineAsHtml(parameter.getDescription()));
        JComponent control = buildControlFor(parameter, info);
        JLabel unit = new JLabel();
        if (parameter.getUnit() != null)
            unit.setText(parameter.getUnit());
        return new JComponent[] {label, control, unit};
    }

    public JComponent buildControlFor(IParameter parameter, IProcessingInfo info) {
        Class<?> type = parameter.getType();
        if (type.equals(Double.class) || type.equals(Double.TYPE) || type.equals(Float.class) || type.equals(Float.TYPE))
            return new DoubleControl(parameter, info);
        else if (type.equals(Long.class) || type.equals(Long.TYPE) ||
                type.equals(Integer.class) || type.equals(Integer.TYPE) ||
                type.equals(Short.class) || type.equals(Short.TYPE) ||
                type.equals(Byte.class) || type.equals(Byte.TYPE))
            return new LongControl(parameter, info);
        else if (Enum.class.isAssignableFrom(type))
            return new EnumControl(parameter);
        else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE))
            return new BooleanControl(parameter);
        throw new UnsupportedOperationException("Unsupported parameter type: " + type.getSimpleName() + " (" + type + ")");
    }

    public static String formatMultiLineAsHtml(String text) {
        if (text == null || text.trim().equals(""))
            return null;
        boolean firstLine = true;
        StringBuilder result = new StringBuilder();
        result.append("<html><body><b>");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                if (firstLine)
                    result.append("</b>");
                result.append("<br>");
                firstLine = false;
            } else if (ch == '<') {
                result.append("&lt;");
            } else if (ch == '>') {
                result.append("&gt;");
            } else if (ch == '"') {
                result.append("&quot;");
            } else if (ch == '&') {
                result.append("&amp;");
            } else {
                result.append(ch);
            }
        }
        if (firstLine)
            result.append("</b>");
        result.append("</body></html>");
        return result.toString();
    }

}
