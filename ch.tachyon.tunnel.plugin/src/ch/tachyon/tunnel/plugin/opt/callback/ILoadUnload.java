package ch.tachyon.tunnel.plugin.opt.callback;

import ch.tachyon.tunnel.plugin.*;

public interface ILoadUnload {

    /**
     * Load this effect. This method will be invoked before any parameter is set and any processing
     * is started.
     * <p>
     * The plugin can use the passed {@link IProcessingInfo} to get the number of channels and the sample
     * rate of the audio that will be processed.
     * <p>
     * Each call of this method is matched with a call to {@link #unload()}.
     */
    public void load(IProcessingInfo info);

    /**
     * Unload this effect. No processing will be made and no parameter will be changed before
     * this method is invoked, unless {@link #load(IProcessingInfo)} is called again.
     * <p>
     * Implementation must free any resources allocated by {@link #load(IProcessingInfo)}.
     * They must also clear any references to the {@link IProcessingInfo} object.
     */
    public void unload();

}
