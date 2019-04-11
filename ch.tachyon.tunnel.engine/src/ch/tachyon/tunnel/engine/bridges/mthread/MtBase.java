package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.bridges.chan.expand.*;
import ch.tachyon.tunnel.engine.bridges.mthread.serial.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.utils.*;

/**
 * Base class for wrapping an {@link IPlugin} subclass into a parallel version.
 */
public abstract class MtBase<E extends IPlugin> implements IPlugin, IStartStop, IBeginProcessing, ISerialSectionHandler, IProcessedFramesMonitor {

    private final static int DEFAULT_CHUNK_LENGTH = 2048;

    public final static String DEBUG_PROPERTY = "ch.tachyon.tunnel.debug";
    public final static String PROFILE_PROPERTY = "ch.tachyon.tunnel.profile";

    private final static String READ_SECTION_NAME = "ch.tachyon.tunnel.engine.source.reader";
    private final static String WRITE_SECTION_NAME = "ch.tachyon.tunnel.engine.sink.writer";
    private final static String HISTORY_SECTION_NAME = "ch.tachyon.tunnel.engine.history";

    private final E target;
    private final boolean needsReadSection;
    protected final MtContext mtContext;

    private final Map<Thread, E> pool = new ConcurrentHashMap<Thread, E>(8, 0.75f, 1);
    private int poolSize;
    private int minInputPoolSize = 0;
    protected ThreadPool threadPool;
    protected ISerialSectionExt[] serialSections;
    protected ISerialSectionExt readSection;
    protected ISerialSectionExt writeSection;
    
    protected int historySize;
    protected int pluginFixedSize;
    protected ConcurrentHistoryBuffer historyBuffer;
    protected ISerialSection historySection;

    // true to disable parallel processing if the history size is too big
    protected boolean serialize;

    protected AtomicLong framesCounter;


    static {
        if (System.getProperty(DEBUG_PROPERTY) != null && !System.getProperty(DEBUG_PROPERTY).equalsIgnoreCase("false"))
            Debug.warn("Debug mode enabled");
    }

    protected MtBase(E target, boolean needsReadSection, MtContext mtContext) {
        this.target = target;
        this.needsReadSection = needsReadSection;
        this.mtContext = mtContext;
    }
    
    /**
     * Overriden to create a pool of processing copies of the plugin.
     */
    public void startProcessing(IProcessingInfo info) {
        // This will invoke startProcessing on all plugins of the pool
        // This will also handle the IHasSerialSection interface
        setupPool(target, info);
    }

    private ISerialSectionExt newSerialSection(String name, Object userData) {
        if (System.getProperty(DEBUG_PROPERTY) != null && !System.getProperty(DEBUG_PROPERTY).equalsIgnoreCase("false"))
            return new DebugSerialSection(threadPool, name, userData);
        else if (System.getProperty(PROFILE_PROPERTY) != null && !System.getProperty(PROFILE_PROPERTY).equalsIgnoreCase("false"))
            return new ProfiledSerialSection(threadPool, name, userData);
        return new SerialSectionImpl(threadPool, name, userData);
    }

    /**
     * Create a new pool of plugins for parallel processing.
     * <p>
     * {@link IStartStop#startProcessing(IProcessingInfo)} is invoked if the
     * {@link IStartStop} interface is implemented by the plugin. It is also
     * invoked on all plugin that could not be created using <code>clone()</code>.
     * <p>
     * If the plugin implements {@link IHasSerialSections}, the corresponding
     * {@link ISerialSection} are created.
     * <p>
     * The plugin to use for a thread of the {@link ThreadPool} can be obtained
     * using {@link #getProcessingTarget(Thread)}.
     */
    private void setupPool(E plugin, final IProcessingInfo info) {
        // Create plugin pool and invoke startProcessing on additional plugins
        // Also set serial sections
        final SerialSectionPool ssPool = new SerialSectionPool(info, mtContext);
        int chanDiv = 1;
        if (info instanceof IChanPartInfo)
            chanDiv = ((IChanPartInfo) info).getTotalNbChannels() / info.getNumberOfChannels();
        threadPool = mtContext.setupPool(this, chanDiv);
        if (needsReadSection) {
            readSection = newSerialSection(READ_SECTION_NAME, null);
            ssPool.addSerialSection(READ_SECTION_NAME, readSection);
        }
        this.historySize = 0;
        this.serialize = false;
        Set<Thread> threads = threadPool.getPoolThreads();
        int index = 0;
        for (Thread thread : threads) {
            E poolPlugin;
            if (index == 0) {
                // Use main plugin
                poolPlugin = plugin;
                // Start
                if (plugin instanceof IStartStop)
                    ((IStartStop) plugin).startProcessing(info);
                // Handle time locality
                if (plugin instanceof ITimeLocal) {
                    pluginFixedSize = getFixedChunkLength();
                    int pluginChunkSize = getActualChunkLength();
                    int hostChunkLength = info.getMaxChunkLength(pluginChunkSize);
                    ITimeLocal tl = (ITimeLocal) plugin;
                    int historySize = tl.getRequiredFramesBefore(info);
                    if (historySize != 0) {
                        /*
                         * M: The size of a block prefixed with the history
                         * S: The size of a block (S < M)
                         * n: The number of threads
                         *
                         * For the multi-core version to be faster, the following must hold
                         * M < S * n
                         *
                         * In practice, however, multi-core introduces various slow downs (more memory, synchronization, virtual cores vs physical cores, etc).
                         * The slow down factor is here estimated as 1.1.
                         * The condition for multi-core processing is hence:
                         *
                         * M * 1.1 < S * n
                         */
                        boolean isWorthParallelizing;
                        if (pluginFixedSize > 0)
                            /*
                             * M = pluginFixedSize (the only size the plugin can handle, hence cannot be expanded with historySize)
                             * S = pluginFixedSize - historySize (historySize must be removed here)
                             */
                            isWorthParallelizing = (historySize > 0 && pluginFixedSize * 11 / 10 < (pluginFixedSize - historySize) * threadPool.getPoolSize());
                        else
                            /*
                             * M = hostChunkLength + historySize
                             * S = hostChunkLength
                             */
                            isWorthParallelizing = (historySize > 0 && (historySize + hostChunkLength) * 11 / 10 < hostChunkLength * threadPool.getPoolSize());
                        if (isWorthParallelizing) {
                            // Parallelizing is faster. Do it
                            int historyCapacity = historySize + hostChunkLength;
                            historyBuffer = new ConcurrentHistoryBuffer(historyCapacity, historyCapacity + pluginChunkSize * mtContext.getSerialRunningMaxSkew());
                            historySection = newSerialSection(HISTORY_SECTION_NAME, historyBuffer);
                            ssPool.addSerialSection(HISTORY_SECTION_NAME, historySection);
                            this.historySize = historySize;
                            ((ProcessingInfoBridge) info).setTimeLocal(historySize);
                        } else {
                            // Parallelizing would be slower. So it's best not to parallelize.
                            // Shit, we are already in MtBase.class, which is for parallel execution.
                            // Set a 'serialize' flag, which will result in creatin a pool of one thread only
                            this.serialize = true;
//                            Debug.info("Serializing because of too big history size: {0}", historySize);
                            this.historySize = 0;
                        }
                    }
                }
                // Create serial sections
                if (plugin instanceof IHasSerialSections) {
                    ((IHasSerialSections) plugin).createSerialSections(new ISerialSectionFactory() {

                        public ISerialSection createSerialSection(String name, Object userData) {
                            ISerialSection ss = newSerialSection(name, userData);
                            ssPool.addSerialSection(name, ss);
                            return ss;
                        }

                        public void ensureMinInputHistorySize(int value) {
                            minInputPoolSize = Math.max(minInputPoolSize, value);
                        }

                        public IProcessingInfo getProcessingInfo() {
                            return info;
                        }

                        public IMtContext getMtContext() {
                            return mtContext;
                        }

                    });
                };
                if (serialize)
                    break;
            } else {
                // Create a working copy
                poolPlugin = PluginDuplicator.prepareCopy(plugin, info);
                // Start
                if (poolPlugin instanceof IStartStop && !(poolPlugin instanceof Cloneable))
                    ((IStartStop) poolPlugin).startProcessing(info);
                // Assign serial sections
                ssPool.setThreadIndex(index);
                if (poolPlugin instanceof IHasSerialSections)
                    ((IHasSerialSections) poolPlugin).setSerialSections(ssPool);
            }
            pool.put(thread, poolPlugin);
            index++;
        }
        writeSection = newSerialSection(WRITE_SECTION_NAME, null);
        ssPool.addSerialSection(WRITE_SECTION_NAME, writeSection);
        serialSections = ssPool.getSerialSections().toArray(new ISerialSectionExt[ssPool.getNbSerialSections()]);
        poolSize = pool.size();
    }

    protected int getInputPoolSize() {
        return mtContext.getSerialTotalMaxSkew() + minInputPoolSize;
    }

    protected E getBaseTarget() {
        return this.target;
    }

    /**
     * @return plugin's fixed chunk length, or <code>-1</code> if none
     */
    public int getFixedChunkLength() {
        if (target instanceof IFixedChunkLength)
            return ((IFixedChunkLength) target).getFixedChunkLength();
        return -1;
    }

    /**
     * Get the chunk length. This is either the desired plugin's chunk length, or a default
     * value if the plugin has no preference. This method never returns -1.
     */
    protected int getActualChunkLength() {
        int result = getFixedChunkLength();
        if (result < 0)
            result = DEFAULT_CHUNK_LENGTH;
        return result;
    }

    /**
     * Get the working copy of the plugin for the given thread
     */
    protected E getProcessingTarget(Thread thread) {
        if (poolSize == 1)
            return target;
        return pool.get(thread);
    }

    public void setCounter(AtomicLong framesCounter) {
        this.framesCounter = framesCounter;
    }
    
    public boolean isParallelized() {
        return !serialize;
    }

    public void beginProcessing(final IProcessingInfo info) {
        if (serialize) {
            if (target instanceof IBeginProcessing)
                ((IBeginProcessing) target).beginProcessing(info);
        } else {
            threadPool.executeByEachThread(new Runnable() {

                public void run() {
                    E plugin = getProcessingTarget(Thread.currentThread());
                    if (plugin instanceof IBeginProcessing)
                        ((IBeginProcessing) plugin).beginProcessing(info);
                }
            });
        }
    }

    public void stopProcessing() {
        // This will invoke IStartStop#stopProcessing on all plugins
        // and ILoadUnload#unload on all plugins except the given one.
        flushPool(target);
        // Clear the serial sections' clock (they are ThreadLocals)
        threadPool.executeByEachThread(new Runnable() {

            public void run() {
                for (ISerialSectionExt serialSection : serialSections)
                    serialSection.clearClock();
            }
        });
        mtContext.releasePool(this);
        threadPool = null;
    }

    /**
     * Flush this plugin pool. Invokes {@link IStartStop#stopProcessing()} on all working copies
     * if they implement the {@link IStartStop} interface.
     * <p>
     * Also invoke {@link ILoadUnload#unload()} on working copies (except the original) that implement
     * the {@link ILoadUnload} interface.
     */
    private void flushPool(E main) {
        if (serialize) {
            if (main instanceof IStartStop)
                ((IStartStop) main).stopProcessing();
        } else {
            threadPool.waitForCurrentlyQueuedTasks();
            threadPool.executeByEachThread(new Runnable() {

                public void run() {
                    E plugin = pool.get(Thread.currentThread());
                    if (plugin instanceof IStartStop)
                        ((IStartStop) plugin).stopProcessing();
                }
            });
        }
        for (E plugin : pool.values()) {
            if (plugin != main) {
                if (plugin instanceof ILoadUnload)
                    ((ILoadUnload) plugin).unload();
            }
        }
       pool.clear();
    }

}
