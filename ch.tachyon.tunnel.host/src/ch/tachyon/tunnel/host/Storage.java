package ch.tachyon.tunnel.host;

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
public class Storage {
    
    private static IStorage getStorage() {
        return EngineResolver.getEngine().getStorage();
    }

    public static void storeParameterValues(Collection<IParameter> parameters, OutputStream output) throws IOException, InvocationTargetException {
        getStorage().storeParameterValues(parameters, output);
    }

    public static void loadParameterValues(InputStream input, IPluginBundle bundle, Collection<IParameter> parameters) throws IOException, InvocationTargetException,
            IllegalAccessException, InstantiationException, ClassNotFoundException {
        getStorage().loadParameterValues(input, bundle, parameters);
    }

}
