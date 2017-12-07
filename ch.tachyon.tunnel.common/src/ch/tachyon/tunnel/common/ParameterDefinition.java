package ch.tachyon.tunnel.common;

import java.util.*;

public class ParameterDefinition<P, T> { // WAIT unit test for correctness, also unit test names in presets

    private final Class<P> pluginClass;
    private final Class<T> type;
    private final String name; // English display name

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPED = new HashMap<Class<?>, Class<?>>();

    static {
        PRIMITIVE_TO_WRAPPED.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPED.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPED.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPED.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPED.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPED.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPED.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPED.put(double.class, Double.class);
    }


    /**
     * @param pluginClass the plugin's class
     * @param type the parameter's data type
     * @param name the name of the parameter (the name of the Java property)
     */
    public ParameterDefinition(Class<P> pluginClass, Class<T> type, String name) {
        this.pluginClass = pluginClass;
        this.type = type;
        this.name = name;
    }

    public Class<P> getPluginClass() {
        return pluginClass;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pluginClass == null) ? 0 : pluginClass.hashCode());
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
        ParameterDefinition<?, ?> other = (ParameterDefinition<?, ?>) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (pluginClass == null) {
            if (other.pluginClass != null)
                return false;
        } else if (!pluginClass.equals(other.pluginClass))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ParameterDefinition [pluginClass=" + pluginClass + ", type=" + type + ", name=" + name + "]";
    }

}
