package ch.tachyon.tunnel.test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import ch.tachyon.tunnel.common.utils.*;
import junit.framework.*;

public class ThreadPooledSuite extends TestSuite {

    private static ExecutorService pool = Executors.newFixedThreadPool(poolSize());
    private final static Object lock = new Monitor(ThreadPooledSuite.class.getSimpleName());
    private final boolean noWait;
    private volatile int activeTestDeathCount;
    private static AtomicInteger activeTestThreads = new AtomicInteger(0);


    private static int poolSize() {
        int nbProcs = Runtime.getRuntime().availableProcessors();
        if (nbProcs <= 3)
            return nbProcs;
        return Runtime.getRuntime().availableProcessors() / 2;
    }
    
    public ThreadPooledSuite() {
        noWait = false;
    }

    public ThreadPooledSuite(Class<? extends TestCase> theClass) {
        super(theClass);
        noWait = false;
    }

    public ThreadPooledSuite(Class<? extends TestCase> theClass, boolean noWait) {
        super(theClass);
        this.noWait = noWait;
    }

    public ThreadPooledSuite(String name) {
        super(name);
        noWait = false;
    }
    
    /**
     * Create a {@link ThreadPooledSuite} if there is more than 1 core and enough memory (3 GB). Else return a regular test suite.
     */
    public static TestSuite create(String name) {
        if (Runtime.getRuntime().availableProcessors() > 1 &&
                Runtime.getRuntime().maxMemory() >= 3L * 1000 * 1000 * 1000)
            return new ThreadPooledSuite(name);
        else
            return new TestSuite(name);
    }

    public ThreadPooledSuite(String name, boolean noWait) {
        super(name);
        this.noWait = noWait;
    }

    public ThreadPooledSuite(Class<? extends TestCase> theClass, String name) {
        super(theClass, name);
        noWait = false;
    }

    @Override
    public void run(TestResult result) {
        activeTestDeathCount = 0;
        super.run(result);
        waitUntilFinished();
    }

    @Override
    public void runTest(final Test test, final TestResult result) {
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    activeTestThreads.getAndIncrement();
                    test.run(result);
                } finally {
                    ThreadPooledSuite.this.runFinished();
                }
            }
        };
        pool.submit(runner);
    }
    
    private boolean isFinished() {
        if (noWait)
            return activeTestDeathCount >= testCount() - poolSize() + 1;
        else
            return activeTestDeathCount >= testCount();
    }

    void waitUntilFinished() {
        synchronized (lock) {
            while (!isFinished()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return; // ignore
                }
            }
        }
    }
    
    static void waitAllThreads() {
        synchronized (lock) {
            while (activeTestThreads.get() > 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return; // ignore
                }
            }
        }
    }

    public void runFinished() {
        synchronized (lock) {
            activeTestDeathCount++;
            activeTestThreads.decrementAndGet();
            lock.notifyAll();
        }
    }
}