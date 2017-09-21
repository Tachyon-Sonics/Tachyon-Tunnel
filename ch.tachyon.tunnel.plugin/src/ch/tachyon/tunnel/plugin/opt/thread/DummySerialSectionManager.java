package ch.tachyon.tunnel.plugin.opt.thread;

import java.util.*;

import ch.tachyon.tunnel.plugin.*;

public class DummySerialSectionManager implements ISerialSectionFactory, ISerialSectionPool {

    private final IProcessingInfo info;
    private final Map<String, ISerialSection> pool = new HashMap<String, ISerialSection>();

    public DummySerialSectionManager(IProcessingInfo info) {
        this.info = info;
    }

    public ISerialSection createSerialSection(String name, Object userData) {
        ISerialSection result = new DummySerialSection(name, userData);
        pool.put(name, result);
        return result;
    }

    public void ensureMinInputHistorySize(int value) {
    }

    public IProcessingInfo getProcessingInfo() {
        return info;
    }

    public IMtContext getMtContext() {
        return new DummyMtContext();
    }

    public ISerialSection getSerialSection(String name) {
        return pool.get(name);
    }

    public int getThreadIndex() {
        return 0;
    }

}