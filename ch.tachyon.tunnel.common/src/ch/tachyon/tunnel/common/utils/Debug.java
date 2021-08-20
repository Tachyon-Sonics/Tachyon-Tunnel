package ch.tachyon.tunnel.common.utils;

import java.util.logging.*;

/**
 * Logging and debugging.
 * Provides:
 * <ul>
 * <li>The {@link #isEnabled()} method, which can be used to test for debug vs production mode. This
 * property is based on the presence of the "Debug" property, which can be passed to the java command
 * using <code>-DDebug=true</code>.
 * <li>The {@link #log(Level, String, Object...)}, {@link #info(String, Object...)},
 * {@link #warn(String, Object...)} and {@link #error(Throwable)} methods for logging through the
 * standard Java logging system.
 * </ul>
 */
public class Debug {

    private final static String DEBUG_PROPERTY = "Debug";
    private final static String DebugPackage;
    private final static String DebugClass;

    private final static boolean enabled;


    static {
        DebugClass = Debug.class.getName();
        int lastDot = DebugClass.lastIndexOf('.');
        DebugPackage = DebugClass.substring(0, lastDot);

        String debug = System.getProperty(DEBUG_PROPERTY);
        enabled = debug != null && !debug.equalsIgnoreCase("false");
        if (enabled)
            // Enable assertions
            Debug.class.getClassLoader().setDefaultAssertionStatus(true);
    }

    public static synchronized void log(Level level, String message, Object... args) {
        String[] strArgs = new String[args.length];
        for (int i = 0; i < args.length; i++)
            strArgs[i] = String.valueOf(args[i]);
        String className = "";
        String methodName = "";
        String packageName = "org.corebounce.common.log";
        try {
            // Find the package name of the calling method
            packageName = DebugPackage;
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            int index = 0;
            while (index < stack.length && (stack[index].getClassName().startsWith(DebugClass) ||
                    stack[index].getClassName().startsWith("java.lang.Thread")))
                index++;
            if (index < stack.length) {
                int lastDot = stack[index].getClassName().lastIndexOf('.');
                packageName = stack[index].getClassName().substring(0, lastDot);
            }
            className = stack[index].getClassName();
            methodName = stack[index].getMethodName();
        } catch (Throwable t) {
            // Fix for GCJ that does not implement Thread#getStackTrace
            // className / methodName will not be available
        }
        // Retrieve logger for the calling package
        Logger logger = Logger.getLogger(packageName);
        if (logger.isLoggable(level)) {
            if (args.length == 1 && args[0] instanceof Throwable)
                logger.logp(level, className, methodName, message, (Throwable) args[0]);
            else
                logger.logp(level, className, methodName, message, strArgs);
        }
    }

    public static void debug(String message, Object... args) {
        log(Level.FINE, message, args);
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public static void info(Throwable t) {
        log(Level.INFO, t.getMessage(), t);
    }

    public static void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }

    public static void warn(Throwable t) {
        log(Level.WARNING, t.getMessage(), t);
    }

    public static void error(String message, Object... args) {
        log(Level.SEVERE, message, args);
    }

    public static void error(String message, Throwable t) {
        log(Level.SEVERE, message, t);
    }

    public static void error(Throwable t) {
        log(Level.SEVERE, t.getMessage(), t);
    }

    /**
     * Do nothing. Allow you to put a statement on which you can put a breakpoint
     */
    public static void doNothing() {

    }

    public static boolean isEnabled() {
        return enabled;
    }

}
