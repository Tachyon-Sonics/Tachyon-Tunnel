package ch.tachyon.tunnel.test;

import java.lang.ref.*;
import java.util.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

public class LazyMcPlugin {

    private final IPluginBundle bundle;
    private final String effectName;
    private final int nbChannels;
    private final boolean parallel;
    private final Collection<WeakReference<Class<?>>> classRefs;
    private IPushEffect plugin;
    
    
    public LazyMcPlugin(IPluginBundle bundle, String effectName, int nbChannels, boolean parallel, Collection<WeakReference<Class<?>>> classRefs) {
        this.bundle = bundle;
        this.effectName = effectName;
        this.nbChannels = nbChannels;
        this.parallel = parallel;
        this.classRefs = classRefs;
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
            if (plugin != null && classRefs != null) {
                try {
                    Class<?> pluginClass = Class.forName(plugin.getClassName(), true, bundle.getClassLoader(false));
                    WeakReference<Class<?>> ref = new WeakReference<>(pluginClass);
                    classRefs.add(ref);
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return plugin;
    }
    
    public void release() {
        this.plugin = null;
    }
}
