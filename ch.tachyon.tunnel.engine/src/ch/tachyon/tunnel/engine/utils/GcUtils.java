package ch.tachyon.tunnel.engine.utils;

import java.lang.ref.*;
import java.util.*;

public class GcUtils {

    /**
     * Make our best efforts to force a full garbage collection, including {@link SoftReference}s.
     * <p>
     * The implementation should work on any fully-compliant JVM, and works quite well
     * on non-compliant JVMs as well.
     * <p>
     * It has a bad side-effect though, in many JVM the heap size ({@link Runtime#totalMemory()})
     * will be raised to its maximum allowed value ({@link Runtime#maxMemory()}), or close to it,
     * as a result of this call. On some JVM the {@link Runtime#maxMemory()} may even change.
     * <p>
     * One GC attempt of this method is done by allocating a <code>double[]</code> array of the
     * maximum heap size. If the maximum heap size is more than 16Gb, this attempt will fail and
     * this method is less reliable as only {@link System#gc()} is used.
     * @see #assertGC(Reference)
     */
    public static void forceGC() {
        // Create a SoftReference, as these are freed later than garbage and WeakReferences.
        SoftReference<Object> tester = new SoftReference<>(new Object());
        try {
            int amount = (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 8 + 1));
            // This should fail as we allocate more than the available memory
            @SuppressWarnings("unused")
            double[] toto = new double[amount];
        } catch (OutOfMemoryError ex) {
            // According to the OutOfmemory doc, a compliant JVM must have done its best efforts
            // to free memory, unless there is more than Integer.MAX_VALUE * 8 available memory (16Gb)
        }
        // Not all JVM are that way compliant though...
        // Try brute-force, but with a time-out in case the JVM completely ignores us
        long deadline = System.currentTimeMillis() + 1000;
        while (tester.get() != null && System.currentTimeMillis() < deadline) {
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
        }
    }

    /**
     * Fill the Java heap with dummy objects until an {@link OutOfMemoryError} occurs and then free
     * them. This can be used as a more reliable way of forcing GC than {@link #forceGC()}. However,
     * the dummy objects created by this method might still be in memory when the method leaves.
     * @see #forceGC()
     */
    public static void fillHeap() {
        System.gc();
        final int MIN_SIZE = 4096;
        List<byte[]> filler = new ArrayList<>();
        int size = MIN_SIZE;
        // Fill with increasing size
        while (true) {
            try {
                byte[] data = new byte[size];
                filler.add(data);
                size = size * 2;
                if (size < 0)
                    size = Integer.MAX_VALUE;
            } catch (OutOfMemoryError ex) {
                break;
            }
        }
        // Fill with decreasing size
        while (size > MIN_SIZE) {
            try {
                byte[] data = new byte[size];
                filler.add(data);
            } catch (OutOfMemoryError ex) {
                size = size / 2;
            }
        }
        // Ok, now delete out shit
        filler.clear();
        filler = null;
        // Yes, it is a good time for a gc!
        System.gc();
        System.runFinalization();
    }

    public static boolean assertGC(Reference<?> ref) {
        List<byte[]> alloc = new ArrayList<>();
        int size = 100000;
        for (int i = 0; i < 50; i++) {
            if (ref.get() == null) {
                alloc = null;
                System.gc();
                return true;
            }
            System.gc();
            System.runFinalization();
            try {
                alloc.add(new byte[size]);
                size = (int) (((double) size) * 1.3);
            } catch (OutOfMemoryError error) {
                size = size / 2;
            }
            try {
                if (i % 3 == 0)
                    Thread.sleep(321);
            } catch (InterruptedException t) {
                // ignore
            }
        }
        alloc = null;
        return false;
    }

}
