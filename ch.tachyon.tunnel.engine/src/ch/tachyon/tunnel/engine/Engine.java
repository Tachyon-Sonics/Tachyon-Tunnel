package ch.tachyon.tunnel.engine;

import java.io.*;
import java.net.*;

import ch.tachyon.tunnel.engine.bridges.mthread.*;
import ch.tachyon.tunnel.engine.storage.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.host.engine.IStorage;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class Engine implements IEngine {
    
    private final MtContext mtContext = new MtContext();
    private final IStorage storage = new StorageImpl();

    
    public IPluginBundle loadPluginBundle(File jarFile, boolean external) {
        return new PluginBundleImpl(jarFile, external);
    }

    public IPluginBundle loadPluginBundle(URL jarURL, boolean external) {
        return new PluginBundleImpl(jarURL, external);
    }

    public <I extends IEffect, E extends IPlugin> I getStaticPlugin(Class<I> commIntf, Class<E> pluginClass) {
        return PluginsImpl.getPlugin(commIntf, pluginClass, mtContext);
    }

    public IMtContext getStaticMtcontext() {
        return mtContext;
    }

    public IStorage getStorage() {
        return storage;
    }

}
