package ch.tachyon.tunnel.test;

import java.lang.ref.*;
import java.util.*;

import ch.tachyon.tunnel.engine.utils.*;
import junit.framework.*;


public class MemoryLeakTest extends TestCase {

    private List<WeakReference<Class<?>>> classRefs;
    
    
    public MemoryLeakTest(List<WeakReference<Class<?>>> classRefs) {
        super(MemoryLeakTest.class.getSimpleName());
        this.classRefs = classRefs;
    }

    @Override
    protected void runTest() throws Throwable {
        System.out.println("[Memory leak test]");
        for (WeakReference<Class<?>> classRef : classRefs) {
            Set<Class<?>> displayed = new HashSet<>();
            int count = 0;
            while (classRef.get() != null) {
                if (count % 3 == 0)
                    ThreadUtils.sleep(500);
                if (count < 30)
                    System.gc();
                else if (count < 60) // More agressive
                    GcUtils.forceGC();
                else // Most aggressive (also slow!)
                    GcUtils.fillHeap();
                // It seems that various cleanups occur concurrently on some JVM.
                // It hence helps to leave some time...
                if (count % 3 == 0)
                    ThreadUtils.sleep(1000);
                count++;
                System.out.println("GC attempt " + count + " of 90");
                if (count >= 90) {
                    StringBuilder classList = new StringBuilder();
                    for (WeakReference<Class<?>> classRef0 : classRefs) {
                        Class<?> clazz = classRef0.get();
                        if (clazz != null && !displayed.contains(clazz)) {
                            classList.append("\n - ");
                            classList.append(clazz);
                            displayed.add(clazz);
                        }
                    }
                    String message = "After repeated aggressive attempts to free the plugin's bundle from memory, the following plugin classes are still loaded:" +
                            classList.toString() + "\n" +
                            "Either you are running on a JVM that does not support class GC, or at least one plugin of the bundle has a memory leak.\n" +
                            "Potential (and typical) memory leak causes are:\n" +
                            " - Usage of a ThreadLocal, and failure to call remove() from all using threads\n" +
                            " - Creation of a Thread and failure to stop it\n" +
                            " - Illegal access to host classes or to the tachyon tunnel host or engine classes\n" +
                            " - Reference added to a sticky system class and never removed (service provider, etc)\n" +
                            " - JNI Global (native code) reference (a debugger breakpoint could be the cause)\n" +
                            "   (more generally, any interaction with a debugger/profiler might eventually prevent proper garbage collection)\n" +
                            " - JVM that does not support class garbage collection, or that has the corresponding option disabled\n" +
                            "To help you, you may run the test again with the -DwaitOnLeak JVM option.\n" +
                            "When this option is specified, this code will sleep for ever on a memory leak, allowing you to start a profiler and figure out the problem.";
                    System.out.println(message);
                    System.out.flush();
                    if (System.getProperty("waitOnLeak") != null) {
                        System.out.println("\nwaitOnLeak option was specified. I'm now entering in an endless sleep loop to allow diagnosis of the memory leak.");
                        // Clear everything that can be cleared to prevent false diagnostics
                        displayed.clear();
                        displayed = null;
                        classRefs.clear();
                        classRefs = null;
                        classList = null;
                        message = null;
                        while (true)
                            ThreadUtils.sleep(1000);
                    }
                    System.out.println("*** Memory leak detected ***");
                    assert false : "Memory leak detected. See System.out for more details";
                }
            }
        }
        System.out.println("[OK]");
    }

}
