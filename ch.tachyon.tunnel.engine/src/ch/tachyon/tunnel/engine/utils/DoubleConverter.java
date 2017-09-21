package ch.tachyon.tunnel.engine.utils;

import ch.tachyon.tunnel.plugin.param.*;

/**
 * Convert double values to other types. Used to convert values specified by
 * the {@link Range} annotation
 */
public class DoubleConverter {

    public static Object fromDouble(double value, Class<?> type) {
        if (type.equals(Double.class) || type.equals(Double.TYPE))
            return value;
        else if (type.equals(Float.class) || type.equals(Float.TYPE))
            return (float) value;
        else if (type.equals(Long.class) || type.equals(Long.TYPE))
            return (long) value;
        else if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            return (int) value;
        else if (type.equals(Short.class) || type.equals(Short.TYPE))
            return (short) value;
        else if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            return (byte) value;
        else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE))
            return (value > 0.0);
        else if (Enum.class.isAssignableFrom(type))
            return type.getEnumConstants()[(int) value];
        throw new IllegalArgumentException("Cannot convert double to " + type);
    }

    public static double toDouble(Object value, Class<?> type) {
        if (type.equals(Double.class) || type.equals(Double.TYPE))
            return (Double) value;
        else if (type.equals(Float.class) || type.equals(Float.TYPE))
            return (Float) value;
        else if (type.equals(Long.class) || type.equals(Long.TYPE))
            return (Long) value;
        else if (type.equals(Integer.class) || type.equals(Integer.TYPE))
            return (Integer) value;
        else if (type.equals(Short.class) || type.equals(Short.TYPE))
            return (Short) value;
        else if (type.equals(Byte.class) || type.equals(Byte.TYPE))
            return (Byte) value;
        else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE))
            return Boolean.TRUE.equals(value) ? 1.0 : 0.0;
        else if (Enum.class.isAssignableFrom(type))
            return ((Enum<?>) value).ordinal();
        throw new IllegalArgumentException("Cannot convert double to " + type);
    }

}
