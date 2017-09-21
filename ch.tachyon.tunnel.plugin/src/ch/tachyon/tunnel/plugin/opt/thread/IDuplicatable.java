package ch.tachyon.tunnel.plugin.opt.thread;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.*;

/**
 * If a plugin implements this interface, it will be used to clone an instance in order
 * to create n instance, when running on n cores.
 * <p>
 * This interface is used instead of {@link IPublicCloneable} if both are implemented.
 * Unlike {@link IPublicCloneable} or {@link Cloneable}, this interface allows the
 * implementation to retrieve information from the given {@link IProcessingInfo}.
 */
public interface IDuplicatable {

    /**
     * Create a clone of this plugin for parallel processing.
     * <p>
     * The method is responsible for copying the values of every parameter.^
     * <p>
     * This method is invoked before any processing starts
     */
    public Object duplicate(IProcessingInfo info);

}
