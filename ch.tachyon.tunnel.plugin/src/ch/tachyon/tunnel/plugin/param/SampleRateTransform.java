package ch.tachyon.tunnel.plugin.param;

import ch.tachyon.tunnel.plugin.*;

/**
 * Implementation of {@link IParameterTransform} where the stored value is a real number
 * (usually between 0 and 1 or 0 and 0.5), and the actual value is that number multiplied
 * by the current sample rate, as reported by the host.
 * <p>
 * This allows a frequency ranging from 0 to Nyquist to be stored as a number between 0 and 0.5.
 * <p>
 * Only <code>float</code> and <code>double</code> values are supported.
 * @see Transform
 */
public class SampleRateTransform implements IParameterTransform {

    @SuppressWarnings("unchecked")
    public <E extends Number> E transform(E value, IProcessingInfo info) {
        if (value instanceof Float)
            return (E) (Float) (value.floatValue() * (float) info.getSampleRate());
        else if (value instanceof Double)
            return (E) (Double) (value.doubleValue() * info.getSampleRate());
        else
            throw new UnsupportedOperationException("Number class not supported: " + value.getClass());
    }

    @SuppressWarnings("unchecked")
    public <E extends Number> E untransform(E value, IProcessingInfo info) {
        if (value instanceof Float)
            return (E) (Float) (value.floatValue() / (float) info.getSampleRate());
        else if (value instanceof Double)
            return (E) (Double) (value.doubleValue() / info.getSampleRate());
        else
            throw new UnsupportedOperationException("Number class not supported: " + value.getClass());
    }

}
