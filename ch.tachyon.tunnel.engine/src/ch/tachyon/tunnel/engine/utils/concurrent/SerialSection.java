package ch.tachyon.tunnel.engine.utils.concurrent;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * A section of code for which executions by different {@link Thread} (usually using {@link Task}s submitted
 * to the {@link ThreadPool}) must be
 * <ul>
 * <li>Serialized (one thread at a time)
 * <li>Executed in increasing clock order (logical clock). Only the thread whose clock matches this section's
 * clock can enter the serial section. When leaving the serial section, this section's clock is incremented.
 * </ul>
 * The object is shared among all threads of the pool.
 * <p>
 * The serialized code section must be surrounded by calls to {@link #enterSerialSection(long)} and
 * {@link #leaveSerialSection()}. To prevent deadlocks in the case of an exception this must be implemented
 * using a <code>try</code>-<code>finally</code> block:
 * <pre>
 * ss.beginSerialSection(clock);
 * try {
 *     // Code of the serial section here...
 * } finally {
 *     ss.leaveSerialSection();
 * }
 * </pre>
 * <p>
 * When more than one {@link SerialSection} are used, the code must ensure the following to prevent deadlocks:
 * <ul>
 * <li>Every {@link Thread} (or every {@link Task} when using the {@link ThreadPool}) pass through all the
 * serial sections
 * <li>The serial section are passed through in the same order by every <code>Task</code>
 * </ul>
 * <p>
 * Warning: the implementation assumes that no more than {@link ThreadPool#getPoolSize()} threads are
 * using it concurrently. It may fail when used by more threads, for instance when creating threads manually
 * or not through the {@link ThreadPool}. Usage by {@link Task}s submitted to the {@link ThreadPool}
 * is always safe.
 */
// This version seems constantly slower than the next one
//public class SerialSection {
//
//    private long clock = 0;
//
//
//    public SerialSection() {
//
//    }
//
//    public void enterSerialSection(long threadClock) {
//        synchronized (this) {
//            while (clock < threadClock)
//                ThreadUtils.wait(this);
//        }
//    }
//
//    public void leaveSerialSection() {
//        synchronized (this) {
//            clock++;
//            // Unleash next
//            notifyAll();
//        }
//    }
//
//    public void sync(long threadClock) {
//        enterSerialSection(threadClock);
//        leaveSerialSection();
//    }
//
//}


public class SerialSection {

    private AtomicLong clock = new AtomicLong(0);
    private final AtomicReference<Thread>[] waiters;
    private final int queueSize;
    private final long mask;
    private volatile boolean disposed = false;


    @SuppressWarnings("unchecked")
    public SerialSection(ThreadPool threadPool) {
        int poolSize = threadPool.getPoolSize();
        int qs = 1;
        // Set queue size to next power of two to allow fast modulo
        while (qs < poolSize)
            qs *= 2;
        this.queueSize = qs;
        this.mask = qs - 1;
        this.waiters = new AtomicReference[queueSize];
        for (int i = 0; i < waiters.length; i++)
            waiters[i] = new AtomicReference<Thread>(null);
    }

    /**
     * Enter this serial section. This method will wait until the serial section's clock
     * reaches the given calling thread clock before returning. When this method returns, any
     * other Threads invoking this method will wait until the calling Thread invokes
     * {@link #leaveSerialSection()}.
     * @param threadClock the logical clock of the calling Thread
     */
    public void enterSerialSection(long threadClock) {
        if (disposed)
            throw new IllegalStateException("Disposed");
        if (threadClock == clock.get()) // Quick check
            return; // Success
        int index = (int) (threadClock & mask);
        waiters[index].set(Thread.currentThread());
        while (threadClock != clock.get())
            LockSupport.park();
        waiters[index].set(null);
    }

    /**
     * Leave this serial section. This will increment this serial section's logical clock, and unlock any
     * other Thread waiting to enter this serial section.
     */
    public void leaveSerialSection() {
        long nextClock = clock.incrementAndGet();
        int index = (int) (nextClock & mask);
        Thread next = waiters[index].get();
        if (next != null)
            LockSupport.unpark(next);
    }

    public void sync(long threadClock) {
        enterSerialSection(threadClock);
        leaveSerialSection();
    }
    
    public void dispose() {
        disposed = true;
        for (AtomicReference<Thread> threadRef : waiters) {
            if (threadRef != null) {
                Thread thread = threadRef.get();
                if (thread != null)
                    LockSupport.unpark(thread);
            }
        }
    }    
    
}


//public class SerialSection {
//
//    private AtomicLong clock = new AtomicLong(0);
//    private final Object[] locks;
//    private final int queueSize;
//    private final long mask;
//
//
//    public SerialSection() {
//        int poolSize = ThreadPool.getPoolSize();
//        int qs = 1;
//        // Set queue size to next power of two to allow fast modulo
//        while (qs < poolSize)
//            qs *= 2;
//        this.queueSize = qs;
//        this.mask = qs - 1;
//        this.locks = new Object[queueSize];
//        for (int i = 0; i < locks.length; i++)
//            locks[i] = new Object();
//    }
//
//    /**
//     * Enter this serial section. This method will wait until the serial section's clock
//     * reaches the given calling thread clock before returning. When this method returns, any
//     * other Threads invoking this method will wait until the calling Thread invokes
//     * {@link #leaveSerialSection()}.
//     * @param threadClock the logical clock of the calling Thread
//     */
//    public void enterSerialSection(long threadClock) {
//        int index = (int) (threadClock & mask);
//        Object lock = locks[index];
//        synchronized (lock) {
//            while (threadClock != clock.get()) {
//                try {
//                    lock.wait();
//                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//    }
//
//    /**
//     * Leave this serial section. This will increment this serial section's logical clock, and unlock any
//     * other Thread waiting to enter this serial section.
//     */
//    public void leaveSerialSection() {
//        long nextClock = clock.incrementAndGet();
//        int index = (int) (nextClock & mask);
//        Object nextLock = locks[index];
//        synchronized (nextLock) {
//            nextLock.notify();
//        }
//    }
//
//}

