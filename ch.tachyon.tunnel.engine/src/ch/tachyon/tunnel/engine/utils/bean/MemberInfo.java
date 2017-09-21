package ch.tachyon.tunnel.engine.utils.bean;

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * A reference to a public property (a pair of getter and setter).
 */
public class MemberInfo {

    private final String name;
    private final Method reader;
    private final Method writer;


    MemberInfo(String name, Method reader, Method writer) {
        this.name = name;
        this.reader = reader;
        this.writer = writer;
    }

    public MemberInfo(Class<?> type, String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Specified name is empty or null");
        // Look for property
        String uName = name.substring(0, 1).toUpperCase() + name.substring(1);
        try {
            this.name = name;
            Method readMethod;
            try {
                readMethod = type.getMethod("get" + uName);
            } catch (NoSuchMethodException ex) {
                readMethod = type.getMethod("is" + uName);
            }
            this.reader = readMethod;
            this.writer = type.getMethod("set" + uName, reader.getReturnType());
        } catch (NoSuchMethodException ex2) {
            throw new IllegalArgumentException("Property not found: " + name);
        }
    }

    public Class<?> getType() {
        return reader.getReturnType();
    }

    public Method getReader() {
        return reader;
    }

    public Method getWriter() {
        return writer;
    }

    public Object getValue(Object instance) throws InvocationTargetException {
        try {
            return reader.invoke(instance);
        } catch (IllegalAccessException ex) {
            throw new IllegalAccessError(ex.getMessage());
        }
    }

    public void setValue(Object instance, Object value) throws InvocationTargetException {
        try {
            writer.invoke(instance, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalAccessError(ex.getMessage());
        }
    }

    public String getName() {
        return this.name;
    }

    // Help tools

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (reader != null)
            return reader.getAnnotation(annotationClass);
        else
            return writer.getAnnotation(annotationClass);
    }

    public Annotation[] getAnnotations() {
        if (reader != null)
            return reader.getAnnotations();
        else
            return writer.getAnnotations();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MemberInfo) {
            MemberInfo other = (MemberInfo) obj;
            return this.name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
