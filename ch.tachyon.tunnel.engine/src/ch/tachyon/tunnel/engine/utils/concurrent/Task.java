package ch.tachyon.tunnel.engine.utils.concurrent;

import java.util.concurrent.*;

public abstract class Task implements Runnable {

    private CountDownLatch latch;


    public abstract void run();

    void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    CountDownLatch getLatch() {
        return this.latch;
    }

    void setTerminated() {
        if (latch != null)
            latch.countDown();
    }


}
