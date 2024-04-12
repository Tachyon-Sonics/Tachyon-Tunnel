package ch.tachyon.tunnel.engine.storage;

import java.util.*;

import org.w3c.dom.*;

class RunningCache {
    
    private final IdentityHashMap<Object, Long> objectKeys = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Element> objectNodes = new IdentityHashMap<>();
    private long counter = 0;
    
    
    public RunningCache() {
        
    }
    
    public boolean isDone(Object item) {
        return objectNodes.containsKey(item);
    }
    
    public long getKey(Object item) {
        Long key = objectKeys.get(item);
        if (key == null) {
            key = counter++;
            objectKeys.put(item, key);
        }
        return key;
    }
    
    public Element getNode(Object item) {
        return objectNodes.get(item);
    }
    
    public void add(Object item, Element node) {
        if (objectNodes.containsKey(item))
            throw new IllegalArgumentException("Object already added: " + item);
        objectNodes.put(item, node);
    }
    
    public Collection<Object> getReferencedObjects() {
        return Collections.unmodifiableCollection(this.objectKeys.keySet());
    }
    
}
