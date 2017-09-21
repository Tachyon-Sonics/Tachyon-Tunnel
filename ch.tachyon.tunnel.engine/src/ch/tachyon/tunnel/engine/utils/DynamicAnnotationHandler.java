package ch.tachyon.tunnel.engine.utils;

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Abstract class and factory for the dynnamic creation of {@link Annotation} instances.
 * <p>
 * To create an instance of annotation <code>@Toto</code>, use
 * <pre>
 * Toto annoInstance = DynamicAnnotationHandler.newAnnotation(Toto.class, new DynamicAnnotationHandler() {
 *     public Object getValue(String methodName) {
 *         // Return the value of the given annotation method
 *     }
 * });
 * </pre>
 * Subclass of this class are responsible for dynamically getting the value of the annotation
 * instance. This is done by implementing <i>one</i> of the following methods:
 * <ul>
 * <li>{@link #getValue(String)} to get the value, if you know its type
 * <li>{@link #getValue(String, Class)} to get the value, if you do not know its type
 * <li>{@link #getValueAsString(String)} to get a string representation of the value. This class will
 * convert most strings to the corresponding object, using the following conventions:
 * <ul>
 * <li>All numeric types are converted using the <code>parse</code> method of the corresponding type
 * <li>Boolean type uses {@link Boolean#valueOf(String)}
 * <li>Enumerated type use {@link Enum#valueOf(Class, String)}
 * <li>Class type use {@link Class#forName(String)}
 * <li>Array types split the string using the "," separator, and recursively convert each element.
 * </ul>
 * </ul>
 * Failure to override at least one of the above method will result in exceptions being thrown at
 * runtime.
 */
public abstract class DynamicAnnotationHandler implements InvocationHandler {

    protected final Class<?> annoType;


    public DynamicAnnotationHandler(Class<?> annoType) {
        this.annoType = annoType;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("annotationType"))
            return annoType;
        Object result = getValue(methodName);
        if (result != null)
            return result;
        String valueString = getValueAsString(methodName);
        if (valueString != null) {
            Class<?> type = method.getReturnType();
            result = getValue(valueString, type);
            if (result != null)
                return result;
        }
        result = method.getDefaultValue();
        if (result != null)
            return result;
        handleNullValue(methodName, method.getReturnType());
        throw new NullPointerException("Property " + methodName + " of annotation type " + annoType.getName() + " is null");
    }

    protected void handleNullValue(String methodName, Class<?> type) {
        throw new NullPointerException("Property " + methodName + " of annotation type " + annoType.getName() + " is null");
    }

    protected Object getValue(String valueString, Class<?> type) throws Exception {
        if (type.equals(String.class))
            return valueString;
        else if (type.equals(Class.class)) {
            return Class.forName(valueString);
        } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
            return Double.parseDouble(valueString);
        } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
            return Long.parseLong(valueString);
        } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
            return Integer.parseInt(valueString);
        } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            return Boolean.valueOf(valueString);
        } else if (Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object result = Enum.valueOf((Class) type, valueString);
            return result;
        } else if (type.isArray()) {
            String[] itemStrings = valueString.split(",");
            if (valueString.trim().equals(""))
                itemStrings = new String[0];
            Class<?> cmpType = type.getComponentType();
            Object[] result = (Object[]) Array.newInstance(cmpType, itemStrings.length);
            for (int i = 0; i < itemStrings.length; i++)
                result[i] = getValue(itemStrings[i], cmpType);
            return result;
        }
        throw new UnsupportedOperationException("Unhandled type: " + type);
    }

    protected Object getValue(String annoMethodName) {
        return null;
    }

    protected String getValueAsString(String annoMethodName) {
        throw new UnsupportedOperationException("getValueAsString() not implemented");
    }

    @SuppressWarnings("unchecked")
    public static <E extends Annotation> E newAnnotation(Class<E> type, DynamicAnnotationHandler impl) {
        return (E) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, impl);
    }
}
