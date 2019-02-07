package ch.tachyon.tunnel.utils;

import java.io.*;

public class Utils {

    public final static String NEW_LINE = System.getProperty("line.separator");
    private static int javaMajorVersion;
    private static int javaMinorVersion;


    static {
        String javaVersion = System.getProperty("java.version");
        try {
            String[] tokens = javaVersion.split("\\.");
            javaMajorVersion = Integer.parseInt(tokens[0]);
            javaMinorVersion = Integer.parseInt(tokens[1]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean eq(Object obj1, Object obj2) {
        if (obj1 == obj2)
            return true;
        if (obj1 == null)
            return false;
        return obj1.equals(obj2);
    }

    public static int hashCode(Object obj) {
        if (obj == null)
            return 0;
        return obj.hashCode();
    }

    public static byte[] toByteArray(Object obj) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            oo.flush();
            return bo.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> E fromByteArray(byte[] data, Class<E> type) {
        if (data == null)
            return null;
        ByteArrayInputStream bi = new ByteArrayInputStream(data);
        try {
            ObjectInputStream oi = new ObjectInputStream(bi);
            Object result = oi.readObject();
            if (result == null)
                return null;
            if (!type.isInstance(result))
                return null;
            return (E) result;
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isBelowJava16() {
        return javaMajorVersion <= 1 && javaMinorVersion < 6;
    }
    
    public static boolean isBelowJava17() {
        return javaMajorVersion <= 1 && javaMinorVersion < 7;
    }

    public static boolean isBelowJava18() {
        return javaMajorVersion <= 1 && javaMinorVersion < 8;
    }

    public static boolean isBelowJava19() {
        return javaMajorVersion <= 1 && javaMinorVersion < 9;
    }

}
