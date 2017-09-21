package ch.tachyon.tunnel.host.effect;

import java.lang.annotation.*;

import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.plugin.param.*;

public interface IParameter {

    /**
     * Get the user-friendly name of this parameter.
     */
    public String getDisplayName();

    /**
     * Get the Java name of this parameter
     */
    public String getName();

    public String getDescription();

    public Class<?> getType();

    public String getUnit();

    /**
     * An {@link IParameterTransform} implementation that maps displayed value to internal (stored) values
     * and back. The result must not be <code>null</code> but can be an <code>IdentityTransform</code>.
     */
    public IParameterTransform getTransform();

    public Range getRange();

    public ScaleType getScaleType();

    public <E extends Annotation> E getAnnotation(Class<E> type);

    public Object getValue(IProcessingInfo info);

    /**
     * Set the parameter's value.
     * <p>
     * In case the plugin uses a different representation for the internal value and the displayed value, this
     * method sets the displayed value and converts it to the internal representation, whereas
     * {@link #setInternalValue(Object)} sets the internal value directly.
     * @param value the new parameter value. Its type must match the one given by {@link #getType()}.
     * If {@link #getType()} returns a primitive type, the value must be the corresponding boxed type
     * from <code>java.lang</code>.
     * @throws ClassCastException if the value is not of the correct type
     * @throws IllegalArgumentException if the parameter defines a {@link Range} (see {@link #getRange()})
     * and the given value is not within the range
     */
    public void setValue(Object value, IProcessingInfo info);

    public Object getInternalValue();

    /**
     * Set the value of this parameter.
     * <p>
     * In case the plugin uses a different representation for the internal value and the displayed value,
     * this method sets the internal value whereas {@link #setValue(Object, IProcessingInfo)} sets the
     * displayed value.
     */
    public void setInternalValue(Object value);

}
