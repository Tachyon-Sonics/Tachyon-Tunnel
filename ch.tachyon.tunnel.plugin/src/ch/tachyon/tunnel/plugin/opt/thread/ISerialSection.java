package ch.tachyon.tunnel.plugin.opt.thread;


/**
 * A section of code for which executions by different threads must be
 * <ul>
 * <li>Serialized (one thread at a time)
 * <li>Executed in increasing clock order (logical clock). Only the task whose clock matches this section's
 * clock can enter the serial section. When leaving the serial section, this section's clock is incremented.
 * </ul>
 * The clock is transparently managed by the actual implementation. It is possible to query the serial sections's
 * clock using {@link #getClock()} if needed. It corresponds to the audio chunk number.
 * <p>
 * This class is thread-safe, however methods can only be invoked by a plugin's process method.
 * <p>
 * The serialized code section must be surrounded by calls to {@link #enterSerialSection()} and
 * {@link #leaveSerialSection()}. To prevent deadlocks in the case of an exception this must be implemented
 * using a <code>try</code>-<code>finally</code> block:
 * <pre>
 * ss.beginSerialSection();
 * try {
 *     // Code of the serial section here...
 * } finally {
 *     ss.leaveSerialSection();
 * }
 * </pre>
 * <p>
 * Optinally, an arbitrary Object can be associated to a serial section, which is shared by all plugin
 * instances (see {@link MultiThreading}) that are created for parallel processing.
 * If not used, <code>null</code> can be passed. The object can be retrieved using {@link #getUserData()}.
 * <p>
 * When more than one {@link ISerialSection} are used, the code must ensure the following to prevent deadlocks:
 * <ul>
 * <li>The code always pass through every and the same serial sections
 * <li>The serial section are always passed through in the same order
 * </ul>
 * <p>
 * A serial section can also be used solely for "resynchronizing" the threads using no code in the section:
 * <pre>
 * ss.enterSerialSection();
 * ss.leaveSerialSection();</pre>
 * This will ensure, for each task, that the previous tasks have at least reached the same point.
 */
public interface ISerialSection {

    /**
     * Get the name of this serial section. The name is whatever String was passed to {@link ISerialSectionFactory#createSerialSection(String, Object)}
     * and is not used internally.
     */
    public String getName();

    /**
     * Get the user data associated with this serial section. The user data is whatever Object was passed to
     * {@link ISerialSectionFactory#createSerialSection(String, Object)} and is not used internally.
     * <p>
     * It can be used by user code to store common data that is used during the serial section.
     */
    public Object getUserData();

    /**
     * Type-safe version of {@link #getUserData()}
     */
    public <E> E getUserData(Class<E> expectedType);

    /**
     * Enter this serial section. The current thread will wait until it is its turn and the previous one
     * has left this serial section.
     */
    public void enterSerialSection();

    /**
     * Leave this serial section. The next thread, if waiting to enter it, will be unblocked.
     * <p>
     * This method also increment by 1 the logical clock value returned by {@link #getClock()}.
     */
    public void leaveSerialSection();

    /**
     * Wait until all previous task have reached this point.
     * <p>
     * This is equivalent to
     * <pre>enterSerialSection();
     * leaveSerialSection();
     * </pre>
     * This can be used when there is no code to execute serially, but it is necessary to synchronize the
     * tasks to make sure every task only continues after the previous tasks have reached the same point.
     */
    public void sync();

    /**
     * Get the current logical clock of the thread using this serial section. The value indicates
     * the order in which the current thread will enter the serial section relative to the other
     * tasks.
     * <p>
     * This method can only be invoked from a thread that is using this serial section.
     * Invocations by other threads are not guaranted to get a consistent result (and may even throw an exception)
     * because the clock is typically stored in a {@link ThreadLocal} variable.
     * <p>
     * This method can be invoked both before and within the serial section, and returns the actual logical clock,
     * starting from zero for the very first task.
     * It might also be invoked after the serial section, but in this case it will return the logical clock
     * <b>+ 1</b>.
     */
    public long getClock();

    public void dispose();
}
