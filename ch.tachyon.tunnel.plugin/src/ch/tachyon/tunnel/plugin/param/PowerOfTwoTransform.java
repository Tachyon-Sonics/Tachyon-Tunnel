package ch.tachyon.tunnel.plugin.param;

import ch.tachyon.tunnel.plugin.*;

/**
 * Implementation of {@link IParameterTransform} where the stored value is a number
 * and the actual value displayed to/chosen by the user is the corresponding power of two.
 * <p>
 * For integer and long data types, the reverse transformation rounds to the nearest integer.
 * For real numbers the reverse transformation produces the exact base-2 logarithm.
 */
public class PowerOfTwoTransform implements IParameterTransform {

    private final static double LOG2 = Math.log(2.0);


    @SuppressWarnings("unchecked")
    public <E extends Number> E transform(E value, IProcessingInfo info) {
        if (value == null)
            return null;
        if (value instanceof Long)
            return (E) (Long) (1L << value.longValue());
        else if (value instanceof Integer)
            return (E) (Integer) (1 << value.intValue());
        else if (value instanceof Float)
            return (E) (Float) (float) Math.pow(2.0, value.floatValue());
        else if (value instanceof Double)
            return (E) (Double) Math.pow(2.0, value.doubleValue());
        else
            throw new UnsupportedOperationException("Number class not supported: " + value.getClass());
    }

    @SuppressWarnings("unchecked")
    public <E extends Number> E untransform(E value, IProcessingInfo info) {
        if (value == null)
            return null;
        if (value instanceof Long)
            return (E) (Long) (log2(value.longValue()));
        else if (value instanceof Integer)
            return (E) (Integer) ((int) log2(value.longValue()));
        else if (value instanceof Float)
            return (E) (Float) (float) (Math.log(value.doubleValue()) / LOG2);
        else if (value instanceof Double)
            return (E) (Double) (Math.log(value.doubleValue()) / LOG2);
        else
            throw new UnsupportedOperationException("Number class not supported: " + value.getClass());
    }

    private static long log2(long value) {
        value = value * 141L / 100L;
        int log = 0;
        if (value >= (1L << 32)) {
            log += 32;
            value >>>= 32;
        }
        if (value >= (1 << 16)) {
            log += 16;
            value >>>= 16;
        }
        if (value >= (1 << 8)) {
            log += 8;
            value >>>= 8;
        }
        if (value >= (1 << 4)) {
            log += 4;
            value >>>= 4;
        }
        if (value >= (1 << 2)) {
            log += 2;
            value >>>= 2;
        }
        if (value >= 2)
            log++;
        return log;
    }
}
