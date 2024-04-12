package ch.tachyon.tunnel.engine.bridges.mthread.serial;

import java.text.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import ch.tachyon.tunnel.engine.utils.concurrent.*;


public class ProfiledSerialSection implements ISerialSectionExt {

    private final ThreadPool threadPool;
    private AtomicLong clock = new AtomicLong(0);
    private final AtomicReference<Thread>[] waiters;
    private final int queueSize;
    private final long mask;

    protected final ThreadLocal<Long> threadClocks = new ThreadLocal<>();
    protected final String name;
    protected final Object userData;

    private AtomicLong waitTime = new AtomicLong(0);
    private AtomicLong enterTime = new AtomicLong(0);
    private AtomicLong leaveTime = new AtomicLong(0);
    private AtomicLong blockedTime = new AtomicLong(0);
    private AtomicLong serialTime = new AtomicLong(0);
    private AtomicLong parallelTime = new AtomicLong(0);
    private ThreadLocal<Long> lastTime = new ThreadLocal<Long>() {

        @Override
        protected Long initialValue() {
            return System.nanoTime();
        }

    };
    private long counter = 0;


    @SuppressWarnings("unchecked")
    public ProfiledSerialSection(ThreadPool threadPool, String name, Object userData) {
        this.threadPool = threadPool;
        this.name = name;
        this.userData = userData;

        int poolSize = threadPool.getPoolSize();
        int qs = 1;
        // Set queue size to next power of two to allow fast modulo
        while (qs < poolSize)
            qs *= 2;
        this.queueSize = qs;
        this.mask = qs - 1;
        this.waiters = new AtomicReference[queueSize];
        for (int i = 0; i < waiters.length; i++)
            waiters[i] = new AtomicReference<>(null);
}

    public String getName() {
        return this.name;
    }

    public Object getUserData() {
        return this.userData;
    }

    @SuppressWarnings("unchecked")
    public <E> E getUserData(Class<E> expectedType) {
        if (userData == null)
            return null;
        if (expectedType.isInstance(userData))
            return (E) userData;
        throw new ClassCastException("Cannopt cast " + userData.getClass() + " to " + expectedType);
    }

    public void setActive(boolean value) {
        long time = System.nanoTime();
        if (value)
            waitTime.addAndGet(time - lastTime.get());
        else
            parallelTime.addAndGet(time - lastTime.get());
        lastTime.set(time);
    }

    public void enterSerialSection() {
        enterSerialSectionImpl(null);
    }
    
    private boolean enterSerialSectionImpl(Long timeOutNano) {
        long time1 = System.nanoTime();
        parallelTime.addAndGet(time1 - lastTime.get());

        long threadClock = this.threadClocks.get();
        int index = (int) (threadClock & mask);
        waiters[index].set(Thread.currentThread());
        Long deadline = (timeOutNano == null ? null : System.nanoTime() + timeOutNano);
        while (threadClock != clock.get()) {
            long timeW = System.nanoTime();
            enterTime.addAndGet(timeW - time1);

            if (timeOutNano == null)
                LockSupport.park();
            else
                LockSupport.parkNanos(timeOutNano);

            time1 = System.nanoTime();
            blockedTime.addAndGet(time1 - timeW);
            if (deadline != null && time1 >= deadline)
                return false;
        }
        counter++;
        waiters[index].set(null);

        long time2 = System.nanoTime();
        enterTime.addAndGet(time2 - time1);
        lastTime.set(time2);
        return true;
    }

    public boolean tryEnterSerialSection(long timeOutNano) {
        return enterSerialSectionImpl(timeOutNano);
    }

    public void leaveSerialSection() {
        long time1 = System.nanoTime();
        serialTime.addAndGet(time1 - lastTime.get());

        long nextClock = clock.incrementAndGet();
        int index = (int) (nextClock & mask);
        Thread next = waiters[index].get();
        if (next != null)
            LockSupport.unpark(next);
        threadClocks.set(threadClocks.get() + 1);

        long time2 = System.nanoTime();
        leaveTime.addAndGet(time2 - time1);
        lastTime.set(time2);
    }

    public void sync() {
        enterSerialSection();
        leaveSerialSection();
    }

    public long getClock() {
        return threadClocks.get();
    }

    public void setClock(long clock) {
        this.threadClocks.set(clock);
    }

    public synchronized void clearClock() {
        this.threadClocks.remove();

        if (counter != 0) {
            // Dump statistics
            int nbThreads = Math.min(Runtime.getRuntime().availableProcessors(), threadPool.getPoolSize());

            // Divide by number of cores:
            parallelTime.set(parallelTime.get() / nbThreads);
            enterTime.set(enterTime.get() / nbThreads);
            leaveTime.set(leaveTime.get() / nbThreads);
            blockedTime.set(blockedTime.get() / nbThreads);
            serialTime.set(serialTime.get() / nbThreads);
            waitTime.set(waitTime.get() / nbThreads);

            System.out.println("SerialSection \"" + name + "\" statistics:");
            DecimalFormat formatter = new DecimalFormat("#,##0");
            DecimalFormat ratioFtr = new DecimalFormat("#,##0.00");
            long totalTime = parallelTime.get() + blockedTime.get() + serialTime.get() + enterTime.get() + leaveTime.get();
            long runningTime =  parallelTime.get() + serialTime.get() + enterTime.get() + leaveTime.get();
            System.out.println(MessageFormat.format(" Inactive time:  {0}ns ({1}%)",
                    formatter.format(waitTime.get() / counter),
                    ratioFtr.format(waitTime.get() * 100.0 / (double) (waitTime.get() + totalTime))));
            System.out.println(MessageFormat.format(" Active time:    {0}ns ({1}%)",
                    formatter.format(totalTime / counter),
                    ratioFtr.format(totalTime * 100.0 / (double) (waitTime.get() + totalTime))));
            System.out.println(MessageFormat.format("   Blocked time: {0}ns ({1}%)",
                    formatter.format(blockedTime.get() / counter),
                    ratioFtr.format(blockedTime.get() * 100.0 / (double) totalTime)));
            System.out.println(MessageFormat.format("   Outside time: {0}ns ({1}%)",
                    formatter.format((parallelTime.get() + enterTime.get()) / counter),
                    ratioFtr.format((parallelTime.get() + enterTime.get()) * 100.0 / (double) totalTime)));
            System.out.println(MessageFormat.format("     Enter time: {0}ns ({1}%)",
                    formatter.format(enterTime.get() / counter),
                    ratioFtr.format(enterTime.get() * 100.0 / (double) (parallelTime.get() + enterTime.get()))));
            System.out.println(MessageFormat.format("   Inside time:  {0}ns ({1}%)",
                    formatter.format((serialTime.get() + leaveTime.get()) / counter),
                    ratioFtr.format((serialTime.get() + leaveTime.get()) * 100.0 / (double) totalTime)));
            System.out.println(MessageFormat.format("     Leave time: {0}ns ({1}%)",
                    formatter.format(leaveTime.get() / counter),
                    ratioFtr.format(leaveTime.get() * 100.0 / (double) (serialTime.get() + leaveTime.get()))));
            System.out.println(MessageFormat.format(" Optimal ratio: {0}",
                    ratioFtr.format((double) runningTime / (double) (serialTime.get() + enterTime.get() + leaveTime.get()))));
            System.out.println(MessageFormat.format(" Actual ratio:  {0}",
                    ratioFtr.format((double) runningTime * nbThreads / (double) totalTime)));
            resyncTime += blockedTime.get() * nbThreads;
            scheduleTime += (enterTime.get() + leaveTime.get()) * nbThreads;
            counter = 0;
        }
    }

    public void dispose() {
        
    }

    public static long resyncTime;
    public static long scheduleTime;

}
