package ch.tachyon.tunnel.engine.utils.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * A queue that combines the advantages of {@link ArrayBlockingQueue} and {@link ConcurrentLinkedQueue}.
 * The queue will block if it is empty or full, however it is fully concurrent in all other cases.
 */
public class ConcurrentLinkedBlockingQueue<E> {

    private final int capacity;
    private final ConcurrentLinkedQueue<E> target = new ConcurrentLinkedQueue<>();
    private final Lock lock;
    private final Condition notFull;
    private final Condition notEmpty;
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicInteger notFullWaiters = new AtomicInteger();
    private final AtomicInteger notEmptyWaiters = new AtomicInteger();


    public ConcurrentLinkedBlockingQueue(int capacity, boolean fair) {
        this.capacity = capacity;
        this.lock = new ReentrantLock(fair);
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
    }

    public E poll() {
        E result = target.poll();
        if (result == null)
            return null;
        size.decrementAndGet();
        // Notify not full
        if (notFullWaiters.get() > 0) {
            lock.lock();
            try {
                notFull.signal();
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    public E take() throws InterruptedException {
        E result = poll();
        if (result != null)
            return result;
        // Wait not empty
        notEmptyWaiters.incrementAndGet();
        try {
            lock.lock();
            result = poll();
            while (result == null) {
                notEmpty.await();
                result = poll();
            }
        } finally {
            lock.unlock();
        }
        notEmptyWaiters.decrementAndGet();
        return result;
    }

    public boolean offer(E element) {
        int newSize = size.incrementAndGet();
        if (newSize > capacity) {
            // Queue is full
            size.decrementAndGet();
            return false;
        }
        boolean result = target.offer(element);
        assert result;
        // Notify not empty
        if (notEmptyWaiters.get() > 0) {
            lock.lock();
            try {
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    public void put(E element) throws InterruptedException {
        boolean result = offer(element);
        if (result)
            return;
        // Wait not full
        notFullWaiters.incrementAndGet();
        try {
            lock.lock();
            result = offer(element);
            while (!result) {
                notFull.await();
                result = offer(element);
            }
        } finally {
            lock.unlock();
        }
        notFullWaiters.decrementAndGet();
    }

    public int size() {
        return size.get();
    }

    public boolean isEmpty() {
        return size.get() == 0;
    }

    public void clear() {
        target.clear();
        if (notFullWaiters.get() > 0) {
            lock.lock();
            try {
                notFull.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

}
