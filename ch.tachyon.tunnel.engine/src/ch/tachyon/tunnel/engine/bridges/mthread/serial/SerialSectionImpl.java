package ch.tachyon.tunnel.engine.bridges.mthread.serial;

import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * {@link ISerialSection} implementation. Extend the {@link SerialSection} class and uses a {@link ThreadLocal} to
 * maintain the serial section's clock. <code>setClock(long)</code> must be invoked appropriately before running the
 * code using the serial section.
 */
public class SerialSectionImpl extends SerialSection implements ISerialSectionExt {

    private final ThreadLocal<LongReference> clock = new ThreadLocal<LongReference>();
    private final String name;
    private final Object userData;


    public SerialSectionImpl(ThreadPool threadPool, String name, Object userData) {
        super(threadPool);
        this.name = name;
        this.userData = userData;
    }

    public String getName() {
        return this.name;
    }

    public Object getUserData() {
        return this.userData;
    }

    @SuppressWarnings("unchecked")
    public <E> E getUserData(Class<E> expectedType) {
        if (userData == null)
            return null;
        if (expectedType.isInstance(userData))
            return (E) userData;
        throw new ClassCastException("Cannopt cast " + userData.getClass() + " to " + expectedType);
    }

    public void enterSerialSection() {
        super.enterSerialSection(clock.get().get());
    }

    public boolean tryEnterSerialSection(long timeOutNano) {
        return super.enterSerialSection(clock.get().get(), timeOutNano);
    }

    @Override
    public void leaveSerialSection() {
        super.leaveSerialSection();
        LongReference lr = clock.get();
        lr.set(lr.get() + 1);
    }

    public void sync() {
        super.sync(clock.get().get());
        LongReference lr = clock.get();
        lr.set(lr.get() + 1);
    }

    public long getClock() {
        return clock.get().get();
    }

    public void setClock(long clock) {
        LongReference lr = this.clock.get();
        if (lr == null) {
            lr = new LongReference();
            this.clock.set(lr);
        }
        lr.set(clock);
    }

    public void setActive(boolean value) {
        // Nothing to do. This is used for profiling only
    }

    public void clearClock() {
        this.clock.remove();
    }

}
