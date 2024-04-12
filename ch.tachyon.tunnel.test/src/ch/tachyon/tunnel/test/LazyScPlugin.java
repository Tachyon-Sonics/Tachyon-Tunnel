package ch.tachyon.tunnel.test;

import java.lang.ref.*;
import java.util.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

public class LazyScPlugin {

    private final IPluginBundle bundle;
    private final String effectName;
    private final boolean parallel;
    private final Collection<WeakReference<Class<?>>> classRefs;
    private ISingleChanPushEffect plugin;
    
    
    public LazyScPlugin(IPluginBundle bundle, String effectName, boolean parallel, Collection<WeakReference<Class<?>>> classRefs) {
        this.bundle = bundle;
        this.effectName = effectName;
        this.parallel = parallel;
        this.classRefs = classRefs;
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
            if (plugin != null && classRefs != null) {
                try {
                    Class<?> pluginClass = Class.forName(plugin.getClassName(), true, bundle.getClassLoader(false));
                    WeakReference<Class<?>> ref = new WeakReference<Class<?>>(pluginClass);
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
