package ch.tachyon.tunnel.engine;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.engine.utils.bean.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * {@link IParameter} implementation based on introspection of a plugin's getters and setters.
 * Represent a single parameter.
 */
public class PropertyParameter<P extends IPlugin> implements IParameter {

    private final P plugin;
    private final Properties properties;
    private final String propertyPrefix;
    private final Method reader;
    private final Method writer;
    private final Class<?> type;
    private final String javaName;
    private final String displayName;
    private final int order;


    public PropertyParameter(P plugin, MemberInfo property, Properties properties,
            String propertyPrefix) {
        this.plugin = plugin;
        this.reader = property.getReader();
        this.writer = property.getWriter();
        this.type = property.getType();
        this.properties = properties;
        this.propertyPrefix = propertyPrefix;
        String propName = property.getName();
        this.javaName = propName;
        Name nameAnno = getAnnotation(Name.class);
        if (nameAnno != null)
            this.displayName = nameAnno.value();
        else
            this.displayName = PluginToHostWrapper.convertName(propName.substring(0, 1).toUpperCase() + propName.substring(1));
        Order orderAnno = getAnnotation(Order.class);
        if (orderAnno != null)
            this.order = orderAnno.value();
        else
            this.order = 0;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getName() {
        return javaName;
    }

    public int getOrder() {
        return order;
    }

    public Class<?> getType() {
        return this.type;
    }

    public String getDescription() {
        Description description = getAnnotation(Description.class);
        if (description != null)
            return description.value();
        return null;
    }

    public String getUnit() {
        Unit unit = getAnnotation(Unit.class);
        if (unit != null)
            return unit.value();
        return null;
    }

    public IParameterTransform getTransform() {
        Transform transformAnno = getAnnotation(Transform.class);
        if (transformAnno != null) {
            try {
                return transformAnno.value().newInstance();
            } catch (InstantiationException ex) {
                throw new RuntimeException("Cannot instanciate " + transformAnno.value() +
                        " specified by the @Transform annotation on parameter \"" + displayName +
                        "\" of the plugin \"" + plugin.getClass().getName() + "\"", ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Cannot instanciate " + transformAnno.value() +
                        " specified by the @Transform annotation on parameter \"" + displayName +
                        "\" of the plugin \"" + plugin.getClass().getName() + "\"", ex);
            }
        }
        return IdentityTransform.getInstance();
    }

    public Range getRange() {
        return getAnnotation(Range.class);
    }

    public ScaleType getScaleType() {
        Scale scale = getAnnotation(Scale.class);
        if (scale != null)
            return scale.value();
        return ScaleType.LINEAR;
    }

    public <E extends Annotation> E getAnnotation(Class<E> annoType) {
        E result = reader.getAnnotation(annoType);
        if (result != null)
            return result;
        if (isAnnoPropertyPresent(annoType)) {
            return DynamicAnnotationHandler.newAnnotation(annoType,
                    new PropertiesAnnotationHandler(annoType, properties,
                            propertyPrefix + "." + annoType.getSimpleName()));
        }
        return null;
    }

    private boolean isAnnoPropertyPresent(Class<?> annoType) {
        String prefix = propertyPrefix + "." + annoType.getSimpleName();
        for (Object key0 : properties.keySet()) {
            String key = String.valueOf(key0);
            if (key.equals(prefix) || key.startsWith(prefix + "."))
                return true;
        }
        return false;
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
        try {
            return reader.invoke(plugin);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * @throws ClassCastException if the value is not of the correct type, or if the value is not
     * of a primitive type and the {@link ClassLoader} of the value and the <code>ClassLoader</code> of
     * the plugin do not match. <p>The later case can occur for instance if you eroneously construct a
     * {@link PluginBundleImpl} with <code>external=true</code>, but access to the plugin's parameter
     * object type (defined in the plugin bundle) directly.
     */
    public void setInternalValue(Object value) {
        Range range = getRange();
        if (range != null) {
            // Check range
            double doubleValue = DoubleConverter.toDouble(value, type);
            if (doubleValue < range.minValue() || doubleValue > range.maxValue()) {
                // Accept and crop if within float roundoff errors
                if ((float) doubleValue == (float) range.minValue()) {
                    doubleValue = range.minValue();
                } else if ((float) doubleValue == (float) range.maxValue()) {
                    doubleValue = range.maxValue();
                } else {
                    throw new IllegalArgumentException("Value out of range for parameter \"" + getDisplayName() +
                                    "\": " + value + "; " +
                                    "range: [" + DoubleConverter.fromDouble(range.minValue(), type) +
                                    " - " + DoubleConverter.fromDouble(range.maxValue(), type) + "]");
                }
            }
        }
        Class<?> valueClass = value.getClass();
        if (!type.isAssignableFrom(valueClass) && !type.isPrimitive()) {
            if (type.getName().equals(valueClass.getName()))
                throw new ClassCastException("ClassLoader mismatch: " +
                		"the value loaded with ClassLoader \"" + valueClass.getClassLoader() +
                		"\" cannot be assigned to the plugin loaded with ClassLoader \"" +
                		type.getClassLoader() + "\"");
            else
                throw new ClassCastException(valueClass + " is not assignable to " + type);
        }
        try {
            writer.invoke(plugin, value);
        } catch (IllegalArgumentException ex) {
            if (type.isPrimitive())
                throw new ClassCastException(value.getClass() + " is not assignable to " + type);
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        return plugin.getClass().getSimpleName() + "#" + getName();
    }

}
