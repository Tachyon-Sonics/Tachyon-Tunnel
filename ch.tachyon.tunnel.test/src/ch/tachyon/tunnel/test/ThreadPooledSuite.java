package ch.tachyon.tunnel.test;

import java.util.concurrent.*;
import junit.framework.*;

public class ThreadPooledSuite extends TestSuite {

    private static ExecutorService pool = Executors.newFixedThreadPool(poolSize());
    private final boolean noWait;
    private volatile int activeTestDeathCount;


    private static int poolSize() {
        return (Runtime.getRuntime().availableProcessors() + 2) / 2;
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

    synchronized void waitUntilFinished() {
        while (!isFinished()) {
            try {
                wait();
            } catch (InterruptedException e) {
                return; // ignore
            }
        }
    }

    synchronized public void runFinished() {
        activeTestDeathCount++;
        notifyAll();
    }
}