package ch.tachyon.tunnel.utils;

/**
 * Simple named object whose purpose is to replace
 * <pre>
 * Object lock = new Object();
 * </pre>
 * by:
 * <pre>
 * Object lock = new Monitor("myLock");
 * </pre>
 * So that the lock has a name that shows up in debuggers, profilers, etc.
 */
public class Monitor {

    private final String name;


    public Monitor(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
