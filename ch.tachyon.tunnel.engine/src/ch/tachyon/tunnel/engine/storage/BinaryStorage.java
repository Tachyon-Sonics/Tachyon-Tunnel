package ch.tachyon.tunnel.engine.storage;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

/**
 * Loading and saving of arbitrary Java objects.
 * Only public read/write properties are saved. Uses GZIP compressed XML format.
 */
public class BinaryStorage {

    /**
     * Save the given JavaBean object to the given output stream.
     * <p>
     * The object is saved in GZIP'ed XML. Only the state exposed by public getters and setters
     * is saved. In addition, {@link Collection}s, {@link Map}s and arrays are supported.
     * @param obj the JavaBean object to save. References are saved as well, circular references
     * are fully supported.
     * @param name a descriptive name. Can be anything, but the same name must be passed to {@link #load(InputStream, String, ClassLoader)}
     * @param output the output stream to save to. It will be closed by this method.
     */
    public static void save(Object obj, String name, OutputStream output) throws IOException,
            InvocationTargetException {
        BufferedOutputStream boutput = new BufferedOutputStream(output);
        GZIPOutputStream goutput = new GZIPOutputStream(boutput);
        OutputStreamWriter owriter = new OutputStreamWriter(goutput);
        try {
            XmlWriter writer = new XmlWriter();
            writer.saveXml(obj, name, owriter);
        } finally {
            owriter.flush();
            owriter.close();
        }
    }

    public static Object load(InputStream input, String name, ClassLoader classLoader) throws IOException,
            InvocationTargetException, IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        BufferedInputStream binput = new BufferedInputStream(input);
        GZIPInputStream ginput = new GZIPInputStream(binput);
        InputStreamReader ireader = new InputStreamReader(ginput);
        try {
            XmlReader reader = new XmlReader(classLoader);
            return reader.loadXml(ireader, name);
        } finally {
            ireader.close();
        }
    }

}
