package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Interface that allows plugins to load and store data in different scopes.
 * <p>
 * An implementation of this interface is available from {@link IProcessingInfo#getStorage()}.
 * The {@link IProcessingInfo} implementation is available when the plugin implements one of the
 * callback interfaces {@link ILoadUnload}, {@link IStartStop} or {@link IBeginProcessing}.
 * <p>
 * Methods of this interface are synchronized
 * @see ScopeType
 * @see IProcessingInfo
 * @see ILoadUnload
 * @see IStartStop
 */
public interface IStorage {

    /**
     * Store the given data in the given scope using the given key.
     * <p>
     * If <code>data</code> is <code>null</code>, the existing data is removed
     * under the given key.
     * @see ScopeType
     * @see IProcessingInfo
     * @see ILoadUnload
     * @see IStartStop
     */
    public void store(String key, ScopeType scope, Object data);

    /**
     * Retreive data stored in the given scope under the given key
     * @see ScopeType
     * @see IProcessingInfo
     * @see ILoadUnload
     * @see IStartStop
     */
    public <E> E load(String key, ScopeType scope, Class<E> dataType);

    /**
     * This is a shortcut to {@link #store(String, ScopeType, Object) store("", ScopeType.AUDIO_PROCESS, data)}.
     */
    public void setProcessData(Object data);

    /**
     * This is a shortcut to {@link #load(String, ScopeType, Class) load("", ScopeType.AUDIO_PROCESS, data)}.
     */
    public <E> E getProcessData(Class<E> dataType);

}
