package ch.tachyon.tunnel.demo.host.gui.utils;

import java.util.*;
import ch.tachyon.tunnel.common.utils.*;

/**
 * A object that holds a value, and allow listeners to be notified when the value change
 * @param <E> the type of the value
 */
public class ValueHolder<E> {
    
    private E value;
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    
    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        E previous = this.value;
        this.value = value;
        if (!Utils.eq(previous, value)) {
            for (Runnable listener : listeners)
                listener.run();
        }
    }

    public void addValueChangeListener(Runnable runnable) {
        listeners.add(runnable);
    }
    
    public void removeValueChangeListener(Runnable runnable) {
        listeners.remove(runnable);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        ValueHolder<?> other = (ValueHolder<?>) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ValueHolder [value=" + value + "]";
    }
    
}
