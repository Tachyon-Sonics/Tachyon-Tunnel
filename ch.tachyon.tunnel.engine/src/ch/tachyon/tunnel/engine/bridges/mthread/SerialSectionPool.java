package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class SerialSectionPool implements ISerialSectionPool {

    private final IProcessingInfo info;
    private final IMtContext mtContext;
    private final Map<String, ISerialSection> serialSections = new LinkedHashMap<String, ISerialSection>();
    private int threadIndex;


    public SerialSectionPool(IProcessingInfo info, IMtContext mtContext) {
        this.info = info;
        this.mtContext = mtContext;
    }

    public ISerialSection getSerialSection(String name) {
        return serialSections.get(name);
    }

    public void addSerialSection(String name, ISerialSection serialSection) {
        if (serialSections.containsKey(name))
            throw new IllegalArgumentException("Duplicate serial section name " + name);
        serialSections.put(name, serialSection);
    }

    public Collection<ISerialSection> getSerialSections() {
        return serialSections.values();
    }

    public int getNbSerialSections() {
        return serialSections.size();
    }

    void setThreadIndex(int threadIndex) {
        this.threadIndex = threadIndex;
    }

    public int getThreadIndex() {
        return threadIndex;
    }

    public IProcessingInfo getProcessingInfo() {
        return info;
    }

    public IMtContext getMtContext() {
        return mtContext;
    }

}
