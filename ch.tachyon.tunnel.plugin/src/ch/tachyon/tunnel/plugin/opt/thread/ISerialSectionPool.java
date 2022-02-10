package ch.tachyon.tunnel.plugin.opt.thread;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Pool of created serial sections.
 * <p>
 * A previously created serial section can be retrieved by name using {@link #getSerialSection(String)}.
 * @see IHasSerialSections
 */
public interface ISerialSectionPool {

    /**
     * Get the previously created section with the given name.
     * <p>
     * Serial sections are created by the {@link IHasSerialSections#createSerialSections(ISerialSectionFactory)} and
     * retrieved by {@link IHasSerialSections#setSerialSections(ISerialSectionPool)} using the argument.
     * @param name the name of the serial section
     * @return the corresponding serial section, or <code>null</code> if there is no serial section of that name.
     * @see IHasSerialSections
     * @see ISerialSectionFactory
     */
    public ISerialSection getSerialSection(String name);

    /**
     * Get a unique index that identifies the current thread. Each thread
     * from the pool will get a different index.
     * <p>
     * The thread that has index 0 will have {@link IHasSerialSections#createSerialSections(ISerialSectionFactory)}
     * invoked, whereas other threads will have {@link IHasSerialSections#setSerialSections(ISerialSectionPool)} invoked.
     */
    public int getThreadIndex();
    
    /**
     * Get the processing information (number of channels, sample rate, etc).
     * <p>
     * The information are the same as those passed to {@link IStartStop#startProcessing(IProcessingInfo)}
     * or {@link IBeginProcessing#beginProcessing(IProcessingInfo)}.
     */
    public IProcessingInfo getProcessingInfo();

    /**
     * Get the multi-threading context object, which allows you to get information about multi-threaded processing
     */
    public IMtContext getMtContext();

}