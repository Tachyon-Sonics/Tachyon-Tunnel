package ch.tachyon.tunnel.demo.host;

import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;

/**
 * Holds name and description of a plugin as well as the necessary information to load it.
 * <p>
 * Can be used to list many plugins in a list of available plugins, without requiring the plugins
 * themselves (and hence the corresponding bundle(s)) to be loaded.
 */
public class EffectDescriptor implements java.io.Serializable {
    
    private String name;
    private String description;
    private String className;

    
    public EffectDescriptor() {
        
    }
    
    public EffectDescriptor(IEffect effect) {
        this.name = effect.getName();
        this.description = effect.getDescription();
        this.className = effect.getClassName();
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    
    public <E extends IEffect> E loadFromBundle(Class<E> asType, IPluginBundle bundle) {
        return bundle.loadPlugin(asType, className);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EffectDescriptor other = (EffectDescriptor) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

}
