package ch.tachyon.tunnel.plugin.opt.thread;

import java.util.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;


public interface ISerialSectionFactory {

    /**
     * Create a new {@link ISerialSection} with the given name and optional user data.
     * @param name an informative name for this serial section. This name is not used by the underlying
     * implementation, but is used as a key for the {@link Map} passed to
     * {@link IHasSerialSections#setSerialSections(ISerialSectionPool)}.
     * @param userData optional user data (can be <code>null</code>). Whatever object is passed can be
     * retrieved at any time using {@link ISerialSection#getUserData()} by threads using
     * the serial section.
     * @throws IllegalArgumentException if a serial section with the given name was already created with this factory.
     */
    public ISerialSection createSerialSection(String name, Object userData);

    public void ensureMinInputHistorySize(int value);

    /**
     * Get the processing informations (number of channels, sample rate, etc).
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
