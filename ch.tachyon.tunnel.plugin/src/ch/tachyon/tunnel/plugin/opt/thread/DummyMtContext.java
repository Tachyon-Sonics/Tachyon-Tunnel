package ch.tachyon.tunnel.plugin.opt.thread;

public class DummyMtContext implements IMtContext {

    public int getMaxNbThreads() {
        return 1;
    }

    public boolean isMultiCore() {
        return false;
    }

    public void setNbThreads(int nbThreads) throws IllegalStateException {
        throw new IllegalStateException();
    }

    public int getSerialRunningMaxSkew() {
        return 1;
    }

    public int getSerialTotalMaxSkew() {
        return 1;
    }

}
