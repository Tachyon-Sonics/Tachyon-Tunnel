package ch.tachyon.tunnel.engine.bridges.mthread.serial;

import java.util.concurrent.atomic.*;

import ch.tachyon.tunnel.engine.utils.concurrent.*;


/**
 * Debugging version of {@link SerialSectionImpl}. This version is slower but attempts to trap and report
 * common programming errors.
 */
public class DebugSerialSection extends SerialSection implements ISerialSectionExt {

    private final ThreadLocal<Long> clock = new ThreadLocal<>();
    private final String name;
    private final Object userData;

    private final ThreadLocal<Boolean> active = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return false;
        }

    };
    private final ThreadLocal<Boolean> used = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }

    };
    private final ThreadLocal<Integer> ticks = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }

    };
    private final AtomicBoolean entered = new AtomicBoolean(false);
    private final static ThreadLocal<DebugSerialSection> previousSection = new ThreadLocal<>();
    private DebugSerialSection commonPreviousSection = null;


    public DebugSerialSection(ThreadPool threadPool, String name, Object userData) {
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
        enterSerialSectionImpl(null);
    }
    
    private boolean enterSerialSectionImpl(Long timeOutNano) {
        // Check for double invocation
        assert !used.get() : "enterSerialSection() invoked twice by the same Thread without leaveSerialSection()";
        used.set(true);
        // Check for missed clock
        int nTicks = ticks.get();
        if (nTicks == 0)
            assert false : "The section was used more than once during clock " + clock.get();
        else if (nTicks > 1)
            assert false : "The section was missed during clock " + (clock.get() - nTicks + 1);
        ticks.set(0);
        // Check if active
        assert active.get() : "enterSerialSection() invoked while serial section is inactive.\n" +
        		"The probable cause is that the calling code is not a Task submitted to the ThreadPool.";
        // Check
        boolean result = true;
        if (timeOutNano == null)
            super.enterSerialSection(clock.get());
        else
            result = super.enterSerialSection(clock.get(), timeOutNano);
        if (result) {
            // Check for double entrance
            boolean success = entered.compareAndSet(false, true);
            assert success : "enterSerialSection() invoked twice without a matching leaveSerialSection()";
            // Check for correct order
            DebugSerialSection myPrevious = previousSection.get();
            if (commonPreviousSection == null) {
                if (myPrevious != null)
                    commonPreviousSection = myPrevious;
            } else {
                if (myPrevious != null)
                    assert myPrevious == commonPreviousSection : "The SerialSections are not entered by all threads in the same order.\n" +
                		"Current thread entered " + myPrevious.name + " followed by " + name +
                		"\nwhereas a previous thread entered " + commonPreviousSection.name + " followed by " + name;
            }
        }
        return result;
    }

    public boolean tryEnterSerialSection(long timeOutNano) {
        return enterSerialSectionImpl(timeOutNano);
    }

    @Override
    public void leaveSerialSection() {
        previousSection.set(this);
        assert active.get() : "leaveSerialSection() invoked while serial section is inactive " +
                        "The probable cause is that the calling code is not a Task submitted to the ThreadPool.";
        boolean success = entered.compareAndSet(true, false);
        assert success : "leaveSerialSection() invoked without a prior call to enterSerialSection()";
        super.leaveSerialSection();
        assert used.get() : "leaveSerialSection() invoked twice by the same Thread during the same clock " + clock.get();
        used.set(false);
        clock.set(clock.get() + 1);
    }

    public void sync() {
        enterSerialSection();
        leaveSerialSection();
    }

    public long getClock() {
        return clock.get();
    }

    public void setClock(long clock) {
        this.clock.set(clock);
        ticks.set(ticks.get() + 1);
    }

    public void setActive(boolean value) {
        boolean previous = active.get();
        assert previous != value : "setActive(" + value + ") was invoked but active status was already " + value;
        active.set(value);
    }

    public void clearClock() {
        this.clock.remove();
        previousSection.remove();
        active.remove();
        used.remove();
        ticks.remove();
        commonPreviousSection = null;
    }

}
