package ch.tachyon.tunnel.plugin.opt.thread;


/**
 * Dummy {@link ISerialSection} implementation that does not serialize anything. This class
 * is only used when no processing is done in parallel.
 */
public class DummySerialSection implements ISerialSection {

    private final String name;
    private final Object userData;
    private long clock;


    public DummySerialSection() {
        this(null, null);
    }

    public DummySerialSection(String name, Object userData) {
        this.name = name;
        this.userData = userData;
    }

    public String getName() {
        return name;
    }

    public Object getUserData() {
        return userData;
    }

    @SuppressWarnings("unchecked")
    public <E> E getUserData(Class<E> expectedType) {
        if (userData == null)
            return null;
        if (expectedType.isInstance(userData))
            return (E) userData;
        throw new ClassCastException("Cannot cast " + userData.getClass() + " to " + expectedType);
    }

    public void enterSerialSection() {

    }

    public void leaveSerialSection() {
        clock++;
    }

    public void sync() {
        clock++;
    }

    public long getClock() {
        return clock;
    }

    public void dispose() {
        
    }

}
