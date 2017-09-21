package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.plugin.host.*;

/**
 * Interface for plugins that cannot process audio from the beginning to the end. This interface allows
 * a plugin to read and write audio data at arbitrary positions using an {@link IRandomAccessor}
 */
public interface IRandomAccessEffect extends IPlugin {

    /**
     * Do the entire processing
     * @param accessor an accessor used to read and write audio samples
     */
    public void process(IRandomAccessor accessor);

}
