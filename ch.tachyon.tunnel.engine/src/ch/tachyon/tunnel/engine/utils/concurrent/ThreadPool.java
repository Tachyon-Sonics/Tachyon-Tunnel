package ch.tachyon.tunnel.engine.utils.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import ch.tachyon.tunnel.common.utils.*;

public class ThreadPool {

    private int poolSize;
    private int queueSize;
    private ConcurrentLinkedBlockingQueue<Task> taskQueue;
    private Object queueLock = new Monitor("ThreadPool.lock");
    private Thread[] poolThreads;
    private volatile boolean active = true;

    private List<TaskFactory> taskFactories;
    private ThreadLocal<TaskFactory> curFactory = new ThreadLocal<TaskFactory>();
    
    private List<Queue<Task>> executeByEachQueue;


    public ThreadPool(String name) {
        this(name, -1);
    }

    public ThreadPool(String name, int nbThreads) {
        active = true;
        if (nbThreads <= 0)
            nbThreads = Runtime.getRuntime().availableProcessors();
        poolSize = (nbThreads > 1 ? nbThreads : 0);
        queueSize = getDefaultQueueSize(nbThreads);
        taskQueue = new ConcurrentLinkedBlockingQueue<Task>(queueSize, false);
        executeByEachQueue = new ArrayList<Queue<Task>>(poolSize);
        for (int i = 0; i < poolSize; i++)
            executeByEachQueue.add(new ConcurrentLinkedQueue<Task>());
        poolThreads = new Thread[poolSize];
        for (int i = 0; i < poolThreads.length; i++) {
            Thread t = new PoolThread(i, this);
            poolThreads[i] = t;
            t.setName(name + " Thread " + i);
            t.start();
        }
    }
    
    public static int getDefaultQueueSize(int nbThreads) {
        return nbThreads + 10;
    }

    /**
     * Add a task factory to this ThreadPool.
     * <p>
     * A task factory is a task that produces and enqueue other tasks using {@link #submitAsync(Task)}, and
     * whose execution is not Thread safe and hence automatically synchronized.
     * A task factory is never executed concurrently by multiple threads, however it is executed repeatedly as long as
     * the pool is non full and as long as it is not removed by {@link #removeTaskFactory(Task)} or {@link #removeSelfTaskFactory()}.
     * <p>
     * For proper integration with the thread pool, the {@link Task#run()} of a task factory must
     * either submit exactly on {@link Task} per execution, or invoke {@link #removeSelfTaskFactory()}
     * to remove itself. A task factory should never submit itself to the pool.
     * @throws IllegalStateException when running on a single processor machine. Task factories are only supported
     * on multi-core hardware
     */
    public void addTaskFactory(Task task) {
        if (!isMultiCore())
            throw new IllegalStateException("Task factories are not supported when only one CPU core is used");
        synchronized (this) {
            if (taskFactories == null)
                taskFactories = new CopyOnWriteArrayList<TaskFactory>();
            taskFactories.add(new TaskFactory(task));
        }
        processFactories();
    }

    /**
     * Remove the given task factory
     */
    public synchronized boolean removeTaskFactory(Task task) {
        if (taskFactories == null)
            return false;
        boolean result = taskFactories.remove(new TaskFactory(task));
        if (taskFactories.isEmpty())
            taskFactories = null;
        return result;
    }

    /**
     * Call {@link #removeTaskFactory(Task)} with the {@link Task} corresponding to the task
     * factory currently being executed.
     * @throws IllegalStateException if this method is not invoked for a task factory run's method.
     */
    public synchronized void removeSelfTaskFactory() {
        if (taskFactories == null)
            return;
        TaskFactory factory = curFactory.get();
        if (factory == null)
            throw new IllegalStateException("Not running a task factory");
        taskFactories.remove(factory);
        if (taskFactories.isEmpty())
            taskFactories = null;
    }

    static class TaskFactory {
        public final Task task;
        public final AtomicBoolean used = new AtomicBoolean(false);

        public TaskFactory(Task task) {
            this.task = task;
        }

        @Override
        public int hashCode() {
            return task.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            TaskFactory other = (TaskFactory) obj;
            return this.task.equals(other.task);
        }

    }

    /**
     * One thread of the pool
     */
    class PoolThread extends Thread {
        
        final int index;
        final ThreadPool pool;

        public PoolThread(int index, ThreadPool pool) {
            this.index = index;
            this.pool = pool;
        }

        @Override
        public void run() {
            while (active)
                processTask(index, true);
        }
    }

    /**
     * Poll and execute a ready task. This is the method invoked in a loop by the pool threads.
     * @param wait whether to wait for a task to be offered if no task is available. If <code>false</code>
     * and no task is available, this method returns immediately without processing any task.
     * <p>
     * In general, 'wait' is false if and only if this method is invoked by a Thread of this pool
     */
    private void processTask(int index, boolean wait) {
        processFactories();

        // Process queued tasks
        Task task = pollTask(index);
        if (task == null) {
            if (wait) {
                synchronized (queueLock) {
                    task = pollTask(index);
                    while (task == null) {
                        try {
                            queueLock.wait();
                        } catch (InterruptedException ex) {
                            if (!active)
                                return;
                            Debug.warn(ex);
                        }
                        task = pollTask(index);
                    }
                }
            } else
                return;
        }
        // Execute the task
        try {
            task.run();
        } finally {
            // Mark terminated
            task.setTerminated();
        }
    }
    
    /**
     * Poll a task to execute. Return <tt>null</tt> if none
     * @param index the index of the current Thread. The current Thread must be a Thread of this ThreadPool
     */
    private Task pollTask(int index) {
        Task result = pollExecuteByEach(index);
        if (result != null)
            return result;
        return taskQueue.poll();
    }

    /**
     * Process any factories by asking them to create task until the queue is filled
     */
    private void processFactories() {
        // Process Task factories?
        List<TaskFactory> factories = taskFactories;
        if (factories != null) {
            int taskQueueSize = taskQueue.size();
            while (factories != null && (taskQueueSize == 0 || taskQueueSize < queueSize - factories.size())) {
                // Create new tasks if factories are available
                for (TaskFactory factory : factories) {
                    if (factory.used.compareAndSet(false, true)) {
                        // Ok, execute this task factory
                        curFactory.set(factory);
                        factory.task.run();
                        curFactory.remove();
                        boolean success = factory.used.compareAndSet(true, false);
                        assert success;
                        taskQueueSize++;
                        if (taskQueueSize >= queueSize)
                            break;
                    }
                }
                taskQueueSize = taskQueue.size();
                factories = taskFactories;
            }
        }
    }
    
    /**
     * Process any task that must be executed by each thread
     */
    private Task pollExecuteByEach(int index) {
        Queue<Task> eachTasks = executeByEachQueue.get(index);
        return eachTasks.poll();
    }

    /**
     * Schedule a task for execution by adding it in the queue.
     * If the queue is full:
     * <ul>
     * <li>If we are a Thread from the pool, poll and execute tasks from the queue until we can add the new task
     * <li>If we are not a Thread from the pool, wait until the queue is non full
     * </ul>
     */
    private void execute(Task task) {
        Thread t = Thread.currentThread();
        boolean nested = (t instanceof PoolThread) && ((PoolThread) t).pool == this;
        if (nested) {
            PoolThread pt = (PoolThread) t;
            // If we are a thread from the pool, actively execute pooled task until we could offer the given task
            while (!offerTask(task))
                processTask(pt.index, false);
        } else {
            // If we are not a thread from the pool, offer the task, passively waiting that the pool is non full
            try {
                taskQueue.put(task);
                synchronized (queueLock) {
                    queueLock.notify();
                }
            } catch (InterruptedException ex) {
                Debug.warn(ex);
            }
        }
    }
    
    /**
     * Add a task to this ThreadPool if the queue if not full
     */
    private boolean offerTask(Task task) {
        boolean result = taskQueue.offer(task);
        if (!result)
            return false;
        synchronized (queueLock) {
            queueLock.notify();
        }
        return true;
    }

    /**
     * Execute all the given tasks in parallel using this thread pool, and wait for them to terminate.
     * <p>
     * This method is equivalent to:
     * <pre>
     * CountDownLatch latch = {@link #submit(Task...) submit(tasks)};
     * {@link #waitFor(CountDownLatch) waitFor(latch)};
     * </pre>
     * Which Thread of the pool will execute which task is not defined
     */
    public void executeAndWait(Task... tasks) {
        if (poolSize <= 1) {
            // Do not use pooling at all
            for (Task task : tasks)
                task.run();
        } else {
            CountDownLatch latch = new CountDownLatch(tasks.length);
            for (Task task : tasks) {
                task.setLatch(latch);
                execute(task);
            }
            waitFor(latch);
        }
    }

    /**
     * Submit one or more task for execution by this thread pool.
     * <p>
     * One or more tasks (and even all tasks if pooling is disabled) may already have started or
     * terminated when this method returns.
     * @param tasks the tasks to submit for execution
     * @return a {@link CountDownLatch} that can be used to wait for all submitted tasks to terminate. Note
     * that <code>null</code> is returned if pooling is disabled. In that case all tasks have been run and
     * terminated when this method returns. It is still safe to invoke {@link #waitFor(CountDownLatch)} with
     * <code>null</code> though.
     */
    public CountDownLatch submit(Task... tasks) {
        if (poolSize <= 1) {
            // Do not use pooling
            for (Task task : tasks)
                task.run();
            return null;
        } else {
            CountDownLatch latch = new CountDownLatch(tasks.length);
            for (Task task : tasks) {
                task.setLatch(latch);
                execute(task);
            }
            return latch;
        }
    }

    /**
     * Submit a task for execution by this thread pool.
     * <p>
     * When submitted with this method, it is not possible to wait for the task's completion
     * using this class. Hence this method can only be used if your code implements its own
     * completion mechanism, or do not care about completion.
     */
    public void submitAsync(Task task) {
        if (poolSize <= 1)
            // Do not use pooling
            task.run();
        else
            execute(task);
    }

    public void submitFragment(Task task) {
        submitAsync(task);
    }

    /**
     * Wait for one or more tasks submitted by {@link #submit(Task...)} to terminate
     * @param latch the {@link CountDownLatch} returned by {@link #submit(Task...)}. If
     * <code>null</code>, this method returns immediately.
     */
    public void waitFor(CountDownLatch latch) {
        if (latch == null)
            return; // This is returned by submit() when no pooling is active
        Thread t = Thread.currentThread();
        boolean nested = (t instanceof PoolThread) && ((PoolThread) t).pool == this;
        try {
            if (nested) {
                PoolThread pt = (PoolThread) t;
                // If we are a thread from the pool, actively process tasks while waiting
                while (!latch.await(0, TimeUnit.NANOSECONDS))
                    processTask(pt.index, false);
            } else {
                // If we are not a thread from the pool, just wait passively
                latch.await();
            }
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }
    
    /**
     * Wait until all Tasks currently queued to this ThreadPool have started
     */
    public void waitForCurrentlyQueuedTasks() {
        final CountDownLatch latch = new CountDownLatch(1);
        Task end = new Task() {
            
            @Override
            public void run() {
                latch.countDown();
            }
        };
        submitAsync(end);
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

    public int getPoolSize() {
        return Math.max(1, poolSize);
    }

    public boolean isMultiCore() {
        return poolSize > 1;
    }

    public int getIncommingQueueCapacity() {
        return queueSize;
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public boolean isFull() {
        return taskQueue.size() >= queueSize;
    }

    /**
     * Get the {@link Thread}s of this pool.
     * <p>
     * Note: on a single-core machine, no Thread is created and this method return a singleton
     * Set with the current Thread.
     */
    public Set<Thread> getPoolThreads() {
        if (poolSize == 0)
            return Collections.singleton(Thread.currentThread());
        else
            return new HashSet<Thread>(Arrays.asList(poolThreads));
    }

    /**
     * Execute the given {@link Runnable} concurrently by every Thread of this thread pool.
     * This method waits until all threads have executed the Runnable.
     * <p>
     * This can be used to initialize or flush thread-local data.
     * <p>
     * Note that {@link Runnable}s submitted by this method bypass this ThreadPool queue
     * and might hence be executed before Tasks submitted before this call.
     * Use {@link #waitForCurrentlyQueuedTasks()} first to flush the queue if desired.
     */
    public void executeByEachThread(final Runnable runnable) {
        if (poolSize <= 1) {
            runnable.run();
            return;
        }
        
        final CountDownLatch finishLatch = new CountDownLatch(poolSize);
        Task task = new Task() {
            
            @Override
            public void run() {
                runnable.run();
            }
        };
        task.setLatch(finishLatch);
        for (int i = 0; i < poolSize; i++)
            executeByEachQueue.get(i).add(task);
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        try {
            finishLatch.await();
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }
    
    /**
     * Execute each of the given {@link Task} concurrently by a different Thread of this thread pool.
     * This method waits until all threads have executed their corresponding Task.
     * <p>
     * This can be used to initialize or flush thread-local data.
     * <p>
     * Note that {@link Task}s submitted by this method bypass this ThreadPool queue
     * and might hence be executed before Tasks submitted before this call.
     * Use {@link #waitForCurrentlyQueuedTasks()} first to flush the queue if desired.
     */
    public void executeByEachThread(final Task[] tasks) {
        if (tasks.length != Math.max(1, poolSize))
            throw new IllegalArgumentException();
        if (poolSize <= 1) {
            tasks[0].run();
            return;
        }
        
        final CountDownLatch finishLatch = new CountDownLatch(poolSize);
        for (int i = 0; i < poolSize; i++) {
            tasks[i].setLatch(finishLatch);
            executeByEachQueue.get(i).add(tasks[i]);
        }
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
        try {
            finishLatch.await();
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

    public void dispose() {
        active = false;
        if (poolThreads != null) {
            for (Thread t : poolThreads) {
                t.interrupt();
                try {
                    t.join(3000);
                } catch (InterruptedException ex) {
                    Debug.warn(ex);
                }
                if (t.isAlive())
                    Debug.warn("Failed to stop pool thread " + t.getName());
            }
        }
        poolThreads = null;
        taskQueue = null;
        executeByEachQueue = null;
    }

}
