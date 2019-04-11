package ch.tachyon.tunnel.engine.utils;

import java.lang.annotation.*;
import java.util.*;

public class AnnotationUtils {

    public static <E extends Annotation> E getAnnotation(Class<?> clazz, Class<E> annoType, Properties properties) {
        E result = clazz.getAnnotation(annoType);
        if (result == null) {
            if (isAnnoPropertyPresent(annoType, properties)) {
                return DynamicAnnotationHandler.newAnnotation(annoType, new PropertiesAnnotationHandler(annoType, properties, annoType.getSimpleName()));
            }
        }
        return result;
    }

    private static boolean isAnnoPropertyPresent(Class<?> annoType, Properties properties) {
        String prefix = annoType.getSimpleName();
        for (Object key0 : properties.keySet()) {
            String key = String.valueOf(key0);
            if (key.equals(prefix) || key.startsWith(prefix + "."))
                return true;
        }
        return false;
    }

}
