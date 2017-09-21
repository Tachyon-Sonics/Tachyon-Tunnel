package ch.tachyon.tunnel.host;

import java.io.*;
import java.net.*;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.host.engine.IStorage;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Used internally. You should not used this class directly.
 */
public interface IEngine {

    public IPluginBundle loadPluginBundle(File jarFile, boolean external);
    
    public IPluginBundle loadPluginBundle(URL jarURL, boolean external);
    
    public <I extends IEffect, E extends IPlugin> I getStaticPlugin(Class<I> commIntf, Class<E> pluginClass);
    
    public IMtContext getStaticMtcontext();
    
    public IStorage getStorage();
    
}
