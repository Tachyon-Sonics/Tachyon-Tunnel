package ch.tachyon.tunnel.plugin.opt.thread;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;


/**
 * Indicate that multiple plugin instances can be used in parallel to process audio on a multi-core
 * machine, but that the processing has some serial sections. This interface allows a plugin to
 * create and store the necessary serial sections.
 * <p>
 * A serial section corresponds to code that must be
 * <ul>
 * <li>Executed by only one plugin instance at a time
 * <li>Executed in the correct order, meaning that execution for a given audio chunk is done before
 * executions for subsequent audio chunks and after executions for previous audio chunks.
 * </ul>
 * <p>
 * The {@link ISerialSection#getClock()} method can be used during the processing to get the current audio chunk
 * number, counted from zero since the last processing has started, as signalled by
 * {@link IStartStop#startProcessing(IProcessingInfo)}.
 * <p>
 * This interface only makes sense if the plugin is annotated with {@link MultiThreading}.
 * <p>
 * When parallel processing is used, the following workflow is applied:
 * <ul>
 * <li>{@link #createSerialSections(ISerialSectionFactory)} is invoked on the plugin instance.
 * The plugin instance must create the necessary serial sections using the given factory and
 * store them to local variables for use during processing.
 * <li>Multiple additonal instances of the plugin are created (see {@link MultiThreading})
 * <li>{@link #setSerialSections(ISerialSectionPool)} is invoked on the created additional instances with the
 * serial sections created in the first step. They are mapped by name. The instances
 * must store the passed serial sections to local variables for use during processing.
 * This method is <i>not</i> invoked on the instance on which {@link #createSerialSections(ISerialSectionFactory)}
 * was invoked.
 * </ul>
 * An arbitrary object can be associated with a serial section (2nd argument of {@link ISerialSectionFactory#createSerialSection(String, Object)})
 * and retrieved by {@link ISerialSection#getUserData()}. Such an object can be used to store shared data using
 * in the section. It must be created by the plugin instance in {@link #createSerialSections(ISerialSectionFactory)}, and retrieved
 * by the additional instances during {@link #setSerialSections(ISerialSectionPool)}.
 * <p>
 * When multiple audio channels are processed and the plugin implements a single-channel processing interface,
 * this workflow is repeated on each channel. In other words, a serial section only allows synchrnoization within
 * an audio channel, but not between different channels. To synchronize different channels, the plugin must implement
 * a multi-channel processing interface such as {@link IMultiChanEffect}.
 * <p>
 * When no parallel processing is done (for instance when running on a single core machine), the
 * {@link #createSerialSections(ISerialSectionFactory)} method is still invoked, but the
 * factory will return simplified serial section implementations where locking is implemented as a no-op.
 */
public interface IHasSerialSections {

    /**
     * Create the serial sections. The plugin must create as many serial sections as needed using
     * the given factory.
     * <p>
     * This method is invoked after {@link IStartStop#startProcessing(IProcessingInfo)} but before {@link IBeginProcessing#beginProcessing(IProcessingInfo)} if implemented.
     */
    public void createSerialSections(ISerialSectionFactory factory);

    /**
     * Set the {@link ISerialSection} objects that can be used to synchronize the execution in the
     * serial sections. The serial sections can be retrived by name from the given pool.
     * The name passed to {@link ISerialSectionPool#getSerialSection(String)} must match the name
     * that was passed to {@link ISerialSectionFactory#createSerialSection(String, Object)} when
     * the serial section was created
     * <p>
     * This method is invoked after {@link IStartStop#startProcessing(IProcessingInfo)} but before {@link IBeginProcessing#beginProcessing(IProcessingInfo)} if implemented.
     * @param pool an object from which created serial sections can be retrieved
     */
    public void setSerialSections(ISerialSectionPool pool);

}
