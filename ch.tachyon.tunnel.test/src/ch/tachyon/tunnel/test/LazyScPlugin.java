package ch.tachyon.tunnel.test;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

public class LazyScPlugin {

    private final IPluginBundle bundle;
    private final String effectName;
    private final boolean parallel;
    private ISingleChanPushEffect plugin;
    
    
    public LazyScPlugin(IPluginBundle bundle, String effectName, boolean parallel) {
        this.bundle = bundle;
        this.effectName = effectName;
        this.parallel = parallel;
    }
    
    public IPluginBundle getBundle() {
        return bundle;
    }

    public String getEffectName() {
        return effectName;
    }

    public boolean isParallel() {
        return parallel;
    }

    public ISingleChanPushEffect getPlugin() {
        if (plugin == null) {
            if (parallel)
                bundle.getMtContext().setNbThreads(Math.max(4, Runtime.getRuntime().availableProcessors()));
            else
                bundle.getMtContext().setNbThreads(1);
            plugin = bundle.loadPlugin(ISingleChanPushEffect.class, effectName);
        }
        return plugin;
    }
}
