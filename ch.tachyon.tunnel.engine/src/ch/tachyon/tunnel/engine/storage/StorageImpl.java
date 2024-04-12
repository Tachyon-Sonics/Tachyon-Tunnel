package ch.tachyon.tunnel.engine.storage;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

/**
 * Utilities for storing parameter values.
 * <p>
 * The values are serialized using a GZIP'ed XML format.
 * <p>
 * Serialization is only possible for values that are
 * <ul>
 * <li>Primitive types or a boxed equivalent
 * <li>Java objects with public properties (getter and setter pairs). In that case only the public properties
 * are processed, and the object's class must have a public no-arg constructor.
 * <li>Arrays, collections or {@link Map}s of any of the above.
 * </ul>
 * When property values are Java objects, they are recursively processed. Arbitrary graphs of objects
 * can be serialized and deserialized as long as they obey the above rules.
 * @author Nicolas
 *
 */
public class StorageImpl implements IStorage {

    public void storeParameterValues(Collection<IParameter> parameters, OutputStream output) throws IOException, InvocationTargetException {
        Map<String, Object> values = new LinkedHashMap<>();
        for (IParameter param : parameters)
            values.put(param.getDisplayName(), param.getInternalValue());
        BinaryStorage.save(values, "values", output);
    }

    public void loadParameterValues(InputStream input, IPluginBundle bundle,
            Collection<IParameter> parameters) throws IOException, InvocationTargetException,
            IllegalAccessException, InstantiationException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, Object> values = (Map<String, Object>) BinaryStorage.load(input, "values", bundle.getClassLoader(true));
        for (IParameter param : parameters) {
            String name = param.getDisplayName();
            Object value = values.get(name);
            if (value != null && isAssignable(value, param.getType()))
                param.setInternalValue(value);
        }
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPED = new HashMap<>();

    static {
        PRIMITIVE_TO_WRAPPED.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_TO_WRAPPED.put(Character.TYPE, Character.class);
        PRIMITIVE_TO_WRAPPED.put(Byte.TYPE, Byte.class);
        PRIMITIVE_TO_WRAPPED.put(Short.TYPE, Short.class);
        PRIMITIVE_TO_WRAPPED.put(Integer.TYPE, Integer.class);
        PRIMITIVE_TO_WRAPPED.put(Long.TYPE, Long.class);
        PRIMITIVE_TO_WRAPPED.put(Float.TYPE, Float.class);
        PRIMITIVE_TO_WRAPPED.put(Double.TYPE, Double.class);
    }

    private static boolean isAssignable(Object value, Class<?> type) {
        if (type.isInstance(value))
            return true;
        if (type.isPrimitive()) {
            Class<?> wrappedType = PRIMITIVE_TO_WRAPPED.get(type);
            return (wrappedType != null && wrappedType.isInstance(value));
        }
        return false;
    }

}
