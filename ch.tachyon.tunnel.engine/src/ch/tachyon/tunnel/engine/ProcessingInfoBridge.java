package ch.tachyon.tunnel.engine;

import java.util.*;
import ch.tachyon.tunnel.engine.bridges.chan.expand.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class ProcessingInfoBridge implements IProcessingInfo, IChanPartInfo {

    private final ch.tachyon.tunnel.host.IProcessingInfo target;
    private final IStorage storage;
    private final IMtContext mtContext;
    private int timeLocal;
    private int addedLength;
    private int pluginLength;
    private int hostLength;


    public ProcessingInfoBridge(ch.tachyon.tunnel.host.IProcessingInfo target) {
        this(target, null, null);
    }

    public ProcessingInfoBridge(ch.tachyon.tunnel.host.IProcessingInfo target, IStorage storage, IMtContext mtContext) {
        this.target = target;
        this.storage = storage;
        this.mtContext = mtContext;
    }

    public int getTimeLocal() {
        return timeLocal;
    }

    public void setTimeLocal(int historySize) {
        this.timeLocal = historySize;
    }

    public int getNumberOfChannels() {
        return target.getNumberOfChannels();
    }

    public BitSet getChannels() {
        return target.getChannels();
    }

    public double getSampleRate() {
        return target.getSampleRate();
    }

    public int getQuantization() {
        return target.getQuantization();
    }

    public int getMaxChunkLength(int pluginOptimalChunkLength) {
        return adaptHostLength(target.getMaxChunkLength(adaptPluginLength(pluginOptimalChunkLength)));
    }

    public int getHostPreferredChunkLength() {
        return adaptHostLength(target.getHostPreferredChunkLength());
    }

    public int negociateFixedChunkLength(int pluginOptimalChunkLength) {
        return adaptHostLength(target.negociateFixedChunkLength(adaptPluginLength(pluginOptimalChunkLength)));
    }

    public long getLength() {
        return target.getLength();
    }

    public IStorage getStorage() {
        return storage;
    }

    public int getChanIndex() {
        if (target instanceof IChanPartInfo)
            return ((IChanPartInfo) target).getChanIndex();
        return 0;
    }

    public int getTotalNbChannels() {
        if (target instanceof IChanPartInfo)
            return ((IChanPartInfo) target).getTotalNbChannels();
        return target.getNumberOfChannels();
    }

    /**
     * If the plugin implements both {@link ITimeLocal} and {@link IFixedChunkLength}:
     * <ul>
     * <li>We subtract timeLocal to fixedChunkLength when reporting plugin optimal chunk length to the host
     * <li>We add timeLocal when reporting a host chunk length to the plugin (see {@link #adaptHostLength(int)})
     * </ul>
     * In case timeLocal is greater than fixedChunkLength, we choose to use a multiple
     * of fixedChunkLength. This allows blocks sent to the plugin (if the hos accepts the length) to be easy
     * to decompose.
     */
    int adaptPluginLength(int pluginLength) {
        this.pluginLength = pluginLength;
        if (pluginLength < 0)
            return -1;
        addedLength = (timeLocal / pluginLength) * pluginLength;
        int result = pluginLength + addedLength - timeLocal;
        assert result > 0;
        return result;
    }

    int adaptHostLength(int hostLength) {
        this.hostLength = hostLength;
        if (hostLength < 0)
            return -1;
        int result = hostLength + timeLocal - addedLength;
        assert result > 0;
        return result;
    }

    public int getAddedLength() {
        return addedLength;
    }

    public int getPluginLength() {
        return pluginLength;
    }

    public int getHostLength() {
        return hostLength;
    }

    public IMtContext getMtContext() {
        return this.mtContext;
    }

}
