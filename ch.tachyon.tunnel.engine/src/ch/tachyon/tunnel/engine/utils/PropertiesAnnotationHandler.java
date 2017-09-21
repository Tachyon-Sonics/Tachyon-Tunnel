package ch.tachyon.tunnel.engine.utils;

import java.lang.reflect.*;
import java.util.*;

/**
 * {@link DynamicAnnotationHandler} implementation based on a properties file.
 * @author Nicolas
 *
 */
public class PropertiesAnnotationHandler extends DynamicAnnotationHandler implements InvocationHandler {

    private final Properties properties;
    private final String prefix;


    public PropertiesAnnotationHandler(Class<?> annoType, Properties properties, String prefix) {
        super(annoType);
        this.properties = properties;
        this.prefix = prefix;
    }

    @Override
    protected String getValueAsString(String annoMethodName) {
        if (annoMethodName.equals("value")) {
            String result = properties.getProperty(prefix);
            if (result != null)
                return result;
        }
        return properties.getProperty(prefix + "." + annoMethodName);
    }

    @Override
    protected void handleNullValue(String methodName, Class<?> type) {
        throw new RuntimeException("Missing of null property: " + prefix + "." + methodName);
    }

}
