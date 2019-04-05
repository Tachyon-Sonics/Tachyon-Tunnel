package ch.tachyon.tunnel.test;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

public class LazyMcPlugin {

    private final IPluginBundle bundle;
    private final String effectName;
    private final int nbChannels;
    private final boolean parallel;
    private IPushEffect plugin;
    
    
    public LazyMcPlugin(IPluginBundle bundle, String effectName, int nbChannels, boolean parallel) {
        this.bundle = bundle;
        this.effectName = effectName;
        this.nbChannels = nbChannels;
        this.parallel = parallel;
    }
    
    public IPluginBundle getBundle() {
        return bundle;
    }

    public String getEffectName() {
        return effectName;
    }

    public int getNbChannels() {
        return nbChannels;
    }

    public boolean isParallel() {
        return parallel;
    }

    public IPushEffect getPlugin() {
        if (plugin == null) {
            if (parallel)
                bundle.getMtContext().setNbThreads(Math.max(4, Runtime.getRuntime().availableProcessors()));
            else
                bundle.getMtContext().setNbThreads(1);
            plugin = bundle.loadPlugin(IPushEffect.class, effectName);
        }
        return plugin;
    }
}
