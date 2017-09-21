package ch.tachyon.tunnel.plugin.param;

import ch.tachyon.tunnel.plugin.*;

/**
 * Transform between the stored value and the actual value of a parameter
 */
public interface IParameterTransform {

    /**
     * Transform a stored value into an actual value
     */
    public <E extends Number> E transform(E value, IProcessingInfo info);

    /**
     * Transform an actual value into a stored value
     */
    public <E extends Number> E untransform(E value, IProcessingInfo info);

}
