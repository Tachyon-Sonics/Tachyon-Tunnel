package ch.tachyon.tunnel.plugin.opt.callback;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Allow a plugin to be notified just before audio processing.
 * This interface can be used to allocate Thread-local resources required for processing.
 */
public interface IBeginProcessing {

    /**
     * Invoked just before the first call to <code>process()</code>, after all parameters
     * have been set. Can be used to allocate processing-related resources.
     * <p>
     * Unlike {@link IStartStop#startProcessing(IProcessingInfo)}, this
     * is the last method that is invoked before the first audio chunk is processed. Various
     * methods such as {@link ILatency#getLatency(ch.tachyon.tunnel.common.IoDirection)},
     * {@link IFixedChunkLength#getFixedChunkLength()}, etc might be invoked by the host after
     * {@link IStartStop#startProcessing(IProcessingInfo)}, but not
     * after this method.
     * <p>
     * If the plugin supports multi-core processing as specified by the {@link MultiThreading}
     * annotation, this method might be invoked concurrently on all processing instances.
     * Furthermore, for each processing instance, it will be invoked exactly by the thread
     * that will then call <code>process()</code> on this instance. It can hence be used to
     * allocate per-thread local data.
     * <p>
     * Any data created by this method must be released by {@link IStartStop#stopProcessing()};
     * that method will be invoked by the same thread that invoked this method.
     */
    public void beginProcessing(IProcessingInfo info);

}
