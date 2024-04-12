package ch.tachyon.tunnel.test;

import java.lang.management.*;
import java.util.*;

import junit.framework.*;

/**
 * This test is used by {@link PluginBundleTester}. It is invoked once before all tests with an empty Set
 * and just fills the set with the existing threads. It is invoked once again (through the subclass
 * {@link ReleaseBundleTest}) and will test that no new thread has been created.
 */
public class NoNewThreadTest extends TestCase {
    
    protected Set<Long> threadIds;

    
    public NoNewThreadTest(Set<Long> threadIds) {
        this(NoNewThreadTest.class.getSimpleName(), threadIds);
    }
    
    protected NoNewThreadTest(String name, Set<Long> threadIds) {
        super(name);
        this.threadIds = threadIds;
    }

    private static Set<Long> getThreads() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        long[] ids = threadMxBean.getAllThreadIds();
        Set<Long> threadIds = new HashSet<>();
        for (long id : ids)
            threadIds.add(id);
        return threadIds;
    }

    @Override
    protected void runTest() throws Throwable {
        if (threadIds.isEmpty()) {
            threadIds.addAll(getThreads());
            return;
        }
        
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        long[] newThreadIds = threadMxBean.getAllThreadIds();
        StringBuilder message = new StringBuilder();
        for (long id : newThreadIds) {
            if (!threadIds.contains(id)) {
                ThreadInfo thread = threadMxBean.getThreadInfo(id);
                if (thread != null && thread.getThreadName() != null && !thread.getThreadName().equals("Finalizer thread")) // IBM J9
                    message.append("\nA plugin created a thread named " + thread.getThreadName() + " but failed to stop it.");
            }
        }
        assert message.length() == 0 : message.toString();
    }

}
