package ch.tachyon.tunnel.engine.storage;

import java.util.*;
import java.util.prefs.*;

import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.plugin.*;

public class PluginStorage implements IStorage {

    private final static Map<String, Object> hostSessionMap = new HashMap<String, Object>();
    private final Map<String, Object> pluginSessionMap = new HashMap<String, Object>();
    private Map<String, Object> channelProcessMap;

    private final Class<?> pluginClass;
    private Preferences userPrefs;
    private Preferences systemPrefs;


    public PluginStorage(Class<?> pluginClass) {
        this.pluginClass = pluginClass;
    }

    private Preferences userPrefs() {
        if (userPrefs == null)
            userPrefs = Preferences.userNodeForPackage(pluginClass);
        return userPrefs;
    }

    private Preferences systemPrefs() {
        if (systemPrefs == null)
            systemPrefs = Preferences.systemNodeForPackage(pluginClass);
        return systemPrefs;
    }

    public void createChannelStorage() {
        channelProcessMap = new HashMap<String, Object>();
    }

    public void destroyChannelStorage() {
        channelProcessMap = null;
    }

    private Map<String, Object> getMapFor(ScopeType scope) {
        switch (scope) {
        case AUDIO_PROCESS:
            return channelProcessMap;
        case CHANNEL_PROCESS:
            return channelProcessMap;
        case PLUGIN_SESSION:
            return pluginSessionMap;
        case HOST_SESSION:
            return hostSessionMap;
        default:
            throw new UnsupportedOperationException();
        }
    }

    public synchronized void store(String key, ScopeType scope, Object data) {
        if (scope == ScopeType.SYSTEM_PERSISTENT)
            storePrefs(systemPrefs(), key, data);
        else if (scope == ScopeType.USER_PERSISTENT)
            storePrefs(userPrefs(), key, data);
        else {
            Map<String, Object> map = getMapFor(scope);
            if (map == null)
                throw new IllegalStateException("The given scope (" + scope + ") does not exist now");
            map.put(key, data);
        }
    }

    public synchronized <E> E load(String key, ScopeType scope, Class<E> dataType) {
        Object result;
        if (scope == ScopeType.SYSTEM_PERSISTENT)
            result = loadPrefs(systemPrefs(), key, dataType);
        else if (scope == ScopeType.USER_PERSISTENT)
            result = loadPrefs(userPrefs(), key, dataType);
        else {
            Map<String, Object> map = getMapFor(scope);
            if (map == null)
                throw new IllegalStateException("The given scope (" + scope + ") does not exist now");
            result = map.get(key);
        }
        if (result == null)
            return null;
        if (!dataType.isInstance(result))
            throw new ClassCastException("Cannot cast " + result.getClass() + " to " + dataType);
        @SuppressWarnings("unchecked")
        E result0 = (E) result;
        return result0;
    }

    public void setProcessData(Object data) {
        store("", ScopeType.AUDIO_PROCESS, data);
    }

    public <E> E getProcessData(Class<E> dataType) {
        return load("", ScopeType.AUDIO_PROCESS, dataType);
    }

    private void storePrefs(Preferences prefs, String key, Object data) {
        if (data == null)
            prefs.remove(key);
        else if (data instanceof Boolean)
            prefs.putBoolean(key, ((Boolean) data).booleanValue());
        else if (data instanceof Integer)
            prefs.putInt(key, (Integer) data);
        else if (data instanceof Long)
            prefs.putLong(key, (Long) data);
        else if (data instanceof Float)
            prefs.putFloat(key, (Float) data);
        else if (data instanceof Double)
            prefs.putDouble(key, (Double) data);
        else if (data instanceof String)
            prefs.put(key, (String) data);
        else if (data instanceof byte[])
            prefs.putByteArray(key, (byte[]) data);
        else
            throw new IllegalArgumentException("Supported types are byte[], String, Boolean, Integer, Long, Float, Double");
    }

    private Object loadPrefs(Preferences prefs, String key, Class<?> dataType) {
        if (dataType.equals(String.class))
            return prefs.get(key, null);
        else if (dataType.equals(byte[].class))
            return prefs.getByteArray(key, null);
        try {
            Set<String> keys = new HashSet<String>(Arrays.asList(prefs.keys()));
            if (!keys.contains(key))
                return null;
        } catch (BackingStoreException ex) {
            Debug.error(ex);
        }
        if (dataType.equals(Boolean.class) || dataType.equals(boolean.class))
            return prefs.getBoolean(key, false);
        else if (dataType.equals(Integer.class) || dataType.equals(int.class))
            return prefs.getInt(key, 0);
        else if (dataType.equals(Long.class) || dataType.equals(long.class))
            return prefs.getLong(key, 0);
        else if (dataType.equals(Float.class) || dataType.equals(float.class))
            return prefs.getFloat(key, Float.NaN);
        else if (dataType.equals(Double.class) || dataType.equals(double.class))
            return prefs.getDouble(key, Double.NaN);
        return null;
    }

}
