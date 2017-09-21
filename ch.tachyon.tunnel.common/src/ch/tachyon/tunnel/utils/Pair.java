package ch.tachyon.tunnel.utils;

public class Pair<E, F> implements Cloneable, java.io.Serializable {
    
    private static final long serialVersionUID = -5591462038036257716L;

    private final E first;
    private final F second;
    
    
    public Pair(E first, F second) {
        this.first = first;
        this.second = second;
    }
    
    public E getFirst() {
        return this.first;
    }
    
    public F getSecond() {
        return this.second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair<?,?>) {
            Pair<?,?> other = (Pair<?,?>) obj;
            return Utils.eq(this.first, other.first) && Utils.eq(this.second, other.second);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Utils.hashCode(this.first) ^ Utils.hashCode(this.second);
    }
    
    @Override
    public String toString() {
        return "Pair(" + first + "," + second + ")";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Pair<E, F> clone() {
        try {
            return (Pair<E, F>)super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

}
