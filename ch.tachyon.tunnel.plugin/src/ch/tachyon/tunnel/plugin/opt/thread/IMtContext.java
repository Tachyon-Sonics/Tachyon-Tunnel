package ch.tachyon.tunnel.plugin.opt.thread;


/**
 * Gives information related to multithreaded processing on multi-core architectures.
 */
public interface IMtContext {

    /**
     * Get the maximum number of concurrent threads that will be used for processing.
     * <p>
     * In general, the engine will use exactly the given value. In some case though, if
     * the engine detects a slow down due to the use of too many threads and a bottlneck
     * in some serial section resulting in too much lock contention, it may dynamically
     * reduce the number of threads actually used. This is however entirely transparent and
     * you should not make any assumption about it.
     * <p>
     * The returned value may or may not correspond to {@link Runtime#availableProcessors()},
     * as the host may choose not to use all available cores (eventually based on a user defined
     * setting).
     * <p>
     * If the plugin does not have the {@link MultiThreading} annotation, processing
     * with that plugin will use one thread only, even if the returned value is greater than one.
     * Hosts can use <code>IEffect.getAnnotation({@link MultiThreading}.class)</code>
     * to check from the host if a plugin supports multi-threading
     */
    public int getMaxNbThreads();

    /**
     * Whether multi-threaded processing will be used.
     * <p>
     * Same as <code>{@link #getMaxNbThreads()} &gt; 1</code>.
     * @see #getMaxNbThreads()
     */
    public boolean isMultiCore();

    /**
     * Set the maximum number of thread that should be used for processing.
     * <p>
     * Specifying <code>1</code> disables multi-threaded processing. Specifying a value
     * less than or equal to <code>0</code> is the same as setting the value to
     * {@link Runtime#availableProcessors()}.
     * @throws IllegalStateException if processing has started. Furthermore, only the
     * host may call this method. Attempt to invoke it from a plugin will throw the exception.
     * If a plugin does not support multi-threading, it suffices not to put the
     * {@link MultiThreading} annotation on it.
     */
    public void setNbThreads(int nbThreads) throws IllegalStateException;

    /**
     * The maximum advance (plus one) a task can have over another one.
     * <p>
     * At any time, due to scheduling innaccuracies of both the OS and the JVM, a task can
     * reach a point in the code before another task that has a smaller logical clock. This
     * situation however will never occur between two tasks whose logical clocks' difference
     * is greater or equal to the value returned by this method.
     * <p>
     * The returned value can be used to create circular buffers required for concurrent processing.
     * It gives the size of the circular buffer to ensure a task will never use an index before the
     * task previously using that index has finished.
     * <p>
     * The returned value is usually the number of processing thread, unless the engine performs
     * additional synchronization that allow reducing this value. The returned value is always one
     * when a single thread is used.
     */
    public int getSerialRunningMaxSkew();

    /**
     * Similar to {@link #getSerialRunningMaxSkew()}, but also add the maximum number of tasks that
     * can be queued, waiting for execution. This is used internally and there is generally no reason
     * to use this method in a plugin
     */
    public int getSerialTotalMaxSkew();

}
