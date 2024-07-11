package ch.tachyon.tunnel.dsp.utils;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class SimpleCacheMap<K, V> extends HashMap<K, WeakReference<V>> {

    private static final long serialVersionUID = 1L;


    public V putItem(K key, V value) {
        WeakReference<V> previous = super.put(key, new WeakReference<>(value));
        return (previous == null ? null : previous.get());
    }

    public V getItem(K key) {
        WeakReference<V> ref = super.get(key);
        return (ref == null ? null : ref.get());
    }
}
