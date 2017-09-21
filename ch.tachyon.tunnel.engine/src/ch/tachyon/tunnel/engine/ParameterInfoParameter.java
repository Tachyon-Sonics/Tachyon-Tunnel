package ch.tachyon.tunnel.engine;

import java.lang.annotation.*;

import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.param.*;

public class ParameterInfoParameter<P extends IPlugin> implements IParameter {

    private final P plugin;
    private final ParameterInfo<P> target;


    public ParameterInfoParameter(P plugin, ParameterInfo<P> target) {
        this.plugin = plugin;
        this.target = target;
    }

    public String getName() {
        return target.getJavaName();
    }

    public String getDisplayName() {
        return target.getDisplayName();
    }

    public String getDescription() {
        Description description = target.getAnnotation(Description.class);
        if (description == null)
            return null;
        return description.value();
    }

    public String getUnit() {
        Unit unit = target.getAnnotation(Unit.class);
        if (unit == null)
            return null;
        return unit.value();
    }

    public IParameterTransform getTransform() {
        Transform transformAnno = target.getAnnotation(Transform.class);
        if (transformAnno != null) {
            try {
                return transformAnno.value().newInstance();
            } catch (InstantiationException ex) {
                throw new RuntimeException("Cannot instanciate " + transformAnno.value() +
                        " specified by the @Transform annotation on parameter \"" + getDisplayName() +
                        "\" of the plugin \"" + plugin.getClass().getName() + "\"", ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot instanciate " + transformAnno.value() +
                        " specified by the @Transform annotation on parameter \"" + getDisplayName() +
                        "\" of the plugin \"" + plugin.getClass().getName() + "\"", ex);
            }
        }
        return IdentityTransform.getInstance();
    }

    public Range getRange() {
        return target.getAnnotation(Range.class);
    }

    public ScaleType getScaleType() {
        Scale scale = target.getAnnotation(Scale.class);
        if (scale != null)
            return scale.value();
        return ScaleType.LINEAR;
    }

    public <E extends Annotation> E getAnnotation(Class<E> type) {
        return target.getAnnotation(type);
    }

    public Class<?> getType() {
        return target.getType();
    }

    public Object getValue(IProcessingInfo info) {
        Object value = getInternalValue();
        if (value instanceof Number)
            value = getTransform().transform((Number) value, new ProcessingInfoBridge(info));
        return value;
    }

    public void setValue(Object value, IProcessingInfo info) {
        if (value instanceof Number)
            value = getTransform().untransform((Number) value, new ProcessingInfoBridge(info));
        setInternalValue(value);
    }

    public Object getInternalValue() {
        return target.getValue(plugin);
    }

    public void setInternalValue(Object value) {
        Range range = getRange();
        if (range != null) {
            // Check range
            Class<?> type = getType();
            double doubleValue = DoubleConverter.toDouble(value, type);
            if (doubleValue < range.minValue() || doubleValue > range.maxValue())
                throw new IllegalArgumentException("Value out of range: " + value + "; " +
                                "range: [" + DoubleConverter.fromDouble(range.minValue(), type) +
                                " - " + DoubleConverter.fromDouble(range.maxValue(), type) + "]");
        }
        target.setValue(plugin, value);
    }

}
