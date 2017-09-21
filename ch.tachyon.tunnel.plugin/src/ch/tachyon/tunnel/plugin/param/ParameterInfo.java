package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;
import java.util.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.*;

/**
 * Meta-data about a plugin's parameter.
 * <p>
 * This class is used if for some reasons the parameters of a plugin are not specified statically by
 * getters and setters, but dynamically by implementing the {@link IDynamicMetaData} interface.
 * <p>
 * Subclasses must fill all properties and implement the logic for reading and writing the parameter
 */
public abstract class ParameterInfo<P extends IPlugin> implements Cloneable, java.io.Serializable {

    private Class<P> pluginType;
    private String javaName;
    private String displayName;
    private Class<?> type;
    private boolean out;
    private Collection<Annotation> annotations;


    public ParameterInfo() {

    }

    public ParameterInfo(Class<P> pluginType, String javaName, String displayName, Class<?> type) {
        this.pluginType = pluginType;
        this.javaName = javaName;
        this.displayName = displayName;
        this.type = type;
    }

    public ParameterInfo(Class<P> pluginType, String javaName, String displayName, Class<?> type, boolean out,
            Collection<Annotation> annotations) {
        this.pluginType = pluginType;
        this.javaName = javaName;
        this.displayName = displayName;
        this.type = type;
        this.out = out;
        this.annotations = annotations;
    }

    public Class<P> getPluginType() {
        return pluginType;
    }

    public void setPluginType(Class<P> pluginType) {
        this.pluginType = pluginType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public boolean isOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Collection<Annotation> annotations) {
        this.annotations = annotations;
    }

    public <E extends Annotation> E getAnnotation(Class<E> type) {
        if (annotations == null)
            return null;
        for (Annotation anno : annotations) {
            if (type.isInstance(anno)) {
                @SuppressWarnings("unchecked")
                E result = (E) anno;
                return result;
            }
        }
        if (type.equals(Range.class)) {
            @SuppressWarnings("unchecked")
            E result = (E) new Range() {

                public Class<? extends Annotation> annotationType() {
                    return Range.class;
                }

                public double minValue() {
                    return 0.0;
                }

                public double maxValue() {
                    return 1.0;
                }

                public double defaultValue() {
                    return 0.0;
                }
            };
            return result;
        }
        return null;
    }

    public abstract Object getValue(P plugin);

    public abstract void setValue(P plugin, Object value);

    public abstract Object getInternalValue(P plugin);

    public abstract void setInternalValue(P plugin, Object value);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((pluginType == null) ? 0 : pluginType.hashCode());
        result = prime * result + (out ? 1231 : 1237);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParameterInfo<?> other = (ParameterInfo<?>) obj;
        if (annotations == null) {
            if (other.annotations != null)
                return false;
        } else if (!annotations.equals(other.annotations))
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (pluginType == null) {
            if (other.pluginType != null)
                return false;
        } else if (!pluginType.equals(other.pluginType))
            return false;
        if (out != other.out)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ParameterInfo<P> clone() {
        try {
            return (ParameterInfo<P>) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

}