package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * A queue in which elements are enqueued with a logical clock and are dequeued in logical clock order.
 * This class allows both enqueuing ({@link #put(long, Object)}) and dequeing ({@link #take(long)})
 * concurrently by multiple threads.
 * <p>
 * No two items can be enqueued with the same logical clock, and an element must be enqueued for
 * every logical clock, starting with 0. <code>null</code> values are not supported.
 * <p>
 * The capacity allows the elements to be enqueued and dequeued out-of-order in a limited way to support
 * concurrency.
 * As long as the enqueued element's clock is less than the last dequeued element's clock plus the
 * capacity, enqueuing is non-blocking. In other cases, enqueuing will block until the
 * next element to dequeue gets enqueued (if not already enqueued) and dequeued.
 * <p>
 * <b>Warning</b>: no check is done on the given logical clock arguments. Failure to enqueue and dequeue every
 * logical clock's value exactly once is likely to cause a deadlock.
 */
public class ClockedAsynchronousQueue<E> {

    private final int capacity;
    private final AtomicReferenceArray<E> buffers;
    private final AtomicReferenceArray<Thread> parkedProducers;
    private final AtomicReferenceArray<Thread> parkedConsumers;
    private volatile boolean active = true;


    public ClockedAsynchronousQueue(int capacity) {
        this.capacity = capacity;
        buffers = new AtomicReferenceArray<E>(capacity);
        parkedProducers = new AtomicReferenceArray<Thread>(capacity);
        parkedConsumers = new AtomicReferenceArray<Thread>(capacity);
    }

    /**
     * Enqueue the given element for the given clock, waiting until the queue is non-full
     */
    public void put(long clock, E element) {
        if (!active)
            return;
        int index = (int) (clock % capacity);
        try {
            if (buffers.compareAndSet(index, null, element))
                return;
            // Previous buffer not consumed yet - Register us as parked producer
            boolean success = parkedProducers.compareAndSet(index, null, Thread.currentThread());
            assert success;
            // Park until previous buffer is consumed and could be replaced by ours
            while (!buffers.compareAndSet(index, null, element))
                park();
            parkedProducers.set(index, null);
        } catch (InterruptedException ex) {
            return;
        } finally {
            // Notify consumer if it was parked
            Thread waiting = parkedConsumers.get(index);
            if (waiting != null)
                LockSupport.unpark(waiting);
        }
    }

    /**
     * Dequeue the element for the given clock, waiting until the element is enqueued.
     */
    public E take(long clock) {
        if (!active)
            return null;
        int index = (int) (clock % capacity);
        try {
            E result = buffers.getAndSet(index, null);
            if (result != null)
                return result;
            // Buffer not produced yet - Register us as parked consumer
            boolean success = parkedConsumers.compareAndSet(index, null, Thread.currentThread());
            assert success;
            // Park until buffer is produced, and consume it
            result = buffers.getAndSet(index, null);
            while (result == null) {
                park();
                result = buffers.getAndSet(index, null);
            }
            parkedConsumers.set(index, null);
            return result;
        } catch (InterruptedException ex) {
            return null;
        } finally {
            // Notify producer if it was parked
            Thread waiting = parkedProducers.get(index);
            if (waiting != null)
                LockSupport.unpark(waiting);
        }
    }

    /**
     * Dequeue the produced buffer for the given clock if available without waiting, or return <code>null</code> else.
     */
    public E poll(long clock) {
        int index = (int) (clock % capacity);
        E result = buffers.getAndSet(index, null);
        if (result != null) {
            // Notify producer if it was parked
            Thread waiting = parkedProducers.get(index);
            if (waiting != null)
                LockSupport.unpark(waiting);
        }
        return result;
    }
    
    private void park() throws InterruptedException {
        if (active)
            LockSupport.park();
        else
            throw new InterruptedException();
    }

    /**
     * Dispose this queue.
     * All Threads currently or subsequently calling {@link #put(long, Object)} will return immediately without waiting.
     * All Threads currently of subsequently calling {@link #take(long)} will return <tt>null</tt> without waiting.
     */
    public void dispose() {
        active = false;
        // Unlock all threads
        for (int i = 0; i < capacity; i++) {
            Thread t = parkedConsumers.get(i);
            if (t != null)
                LockSupport.unpark(t);
            t = parkedProducers.get(i);
            if (t != null)
                LockSupport.unpark(t);
        }
    }
}
