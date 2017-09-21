package ch.tachyon.tunnel.utils;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

public class IOUtils {

    /**
     * Close a {@link Closeable} by invoking {@link Closeable#close()}. Any checked {@link IOException}
     * is wrapped in an unchecked exception. RuntimeException is used before Java 8, and UncheckedIOException
     * for Java 8 and later.
     */
    public static void close(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (IOException ex) {
            if (Utils.isBelowJava18())
                throw new RuntimeException(ex);
            else {
                try {
                    Class<?> clazz = Class.forName("java.io.UncheckedIOException");
                    Constructor<?> cons = clazz.getConstructor(IOException.class);
                    RuntimeException rex = (RuntimeException) cons.newInstance(ex);
                    throw rex;
                } catch (ClassNotFoundException ex2) {
                    ex2.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (SecurityException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (NoSuchMethodException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (IllegalArgumentException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (InstantiationException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex1) {
                    ex1.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    
    public static void close(Object c) {
        if (c instanceof Closeable)
            close((Closeable) c);
    }

    /**
     * Replacement for the Java 1.5 method <code>Properties.load(Reader)</code> with an UTF-8 {@link Reader}
     * for Java 1.5
     */
    public static void loadUtf8Properties(InputStream input, Properties properties) throws IOException {
        CodecInputStream codec = new CodecInputStream(input,
                Charset.forName("UTF-8"),
                Charset.forName("ISO-8859-1"));
        properties.load(codec);
    }

}
