package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.concurrent.atomic.*;

public interface IProcessedFramesMonitor {

    public void setCounter(AtomicLong framesCounter);

}
