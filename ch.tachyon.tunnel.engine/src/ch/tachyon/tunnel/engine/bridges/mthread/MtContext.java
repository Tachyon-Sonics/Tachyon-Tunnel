package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.*;

import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.utils.*;


public class MtContext implements IMtContext {

    private static final String NB_THREADS_PROPERTY = "ch.tachyon.tunnel.threads";
    
    private int nbThreads;
    private int defaultNbThreads;
    
    private Map<Object, ThreadPool> pools = new HashMap<Object, ThreadPool>();


    synchronized ThreadPool setupPool(Object owner, int chanDivider) {
        ThreadPool pool = pools.get(owner);
        if (pool == null) {
            int nbThreads = getNbThreads();
            if (chanDivider > 1)
                nbThreads = Math.max(1, (nbThreads + chanDivider - 1) / chanDivider);
            pool = new ThreadPool("Processing", nbThreads);
            pools.put(owner, pool);
        }
        return pool;
    }

    synchronized void releasePool(Object owner) {
        ThreadPool pool = pools.remove(owner);
        if (pool != null)
            pool.dispose();
    }

    public int getMaxNbThreads() {
        return getNbThreads();
    }

    public synchronized ThreadPool getThreadPool(Object owner) {
        return pools.get(owner);
    }

    public int getNbThreads() {
        if (nbThreads <= 0) {
            if (defaultNbThreads < 1) {
                String prop = System.getProperty(NB_THREADS_PROPERTY);
                if (prop != null) {
                    try {
                        int result = Integer.parseInt(prop);
                        if (result >= 1) {
                            defaultNbThreads = result;
                            Debug.info("Using {0} threads", result);
                        }
                    } catch (NumberFormatException ex) {
                        Debug.warn("Illegal value for property " + NB_THREADS_PROPERTY + ": {0}", prop);
                    }
                }
                if (defaultNbThreads < 1) {
                    defaultNbThreads = Runtime.getRuntime().availableProcessors();
                    long maxMemMb = (Runtime.getRuntime().maxMemory() >> 20) - 96; // Assume base of 96 Mb
                    int maxSupportedCores = (int) Math.max(1, maxMemMb / 10); // Assume 10Mb required per core
                    if (defaultNbThreads > maxSupportedCores) {
                        Debug.warn("Using only {0} of {1} cores as the allocated memory is limited", maxSupportedCores, defaultNbThreads);
                        defaultNbThreads = maxSupportedCores;
                    }
                }
            }
            return defaultNbThreads; // <-- Change here the number of threads to use
        }
        return nbThreads;
    }

    public boolean isMultiCore() {
        return getNbThreads() > 1;
    }

    public void setNbThreads(int nbThreads) {
        if (!pools.isEmpty() && nbThreads != this.nbThreads)
            throw new IllegalStateException("Cannot change the number of threads while a pool is in use");
        this.nbThreads = nbThreads;
    }

    /**
     * The maximum advance a task can have over another one.
     * <p>
     * At any time, due to scheduling innaccuracies of both the OS and the JVM, a task can
     * reach a point in the code before another task that has a smaller logical clock. This
     * situation however will never occur between two tasks whose logical clocks' difference
     * is greater than the value returned by this method.
     * <p>
     * The returned value can be used to create circular buffers required for concurrent processing.
     * It gives the size of the circular buffer to ensure a task will never use an index before the
     * task previously using that index has finished.
     */
    public int getSerialRunningMaxSkew() {
        return getNbThreads();
    }

    /**
     * Similar to {@link #getSerialRunningMaxSkew()}, but also add the maximum number of tasks that
     * can be queued, waiting for execution. This is used internally and there is generally no reason
     * to use this method in a plugin
     */
    public int getSerialTotalMaxSkew() {
        int nbThreads = getNbThreads();
        return nbThreads + ThreadPool.getDefaultQueueSize(nbThreads);
    }

}
