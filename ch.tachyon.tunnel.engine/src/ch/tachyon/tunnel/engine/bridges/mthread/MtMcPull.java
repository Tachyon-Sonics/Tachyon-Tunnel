package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.bridges.mthread.serial.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;


public class MtMcPull extends MtBase<IMultiChanPullEffect> implements IMultiChanPullEffect, IMultiChanAudioSource {

    volatile boolean active = false;
    boolean started = false;
    private IMultiChanAudioSource source;
    private int nbChans;
    private int length; // Length of the enqueued blocks

    private SourceTask[] taskSources;
    private float[][][] buffers;
    private int taskIndex;

    ClockedAsynchronousQueue<float[][]> queue;

    private AtomicLong producerClock = new AtomicLong();
    private long consumerClock;

    private float[][] curBuffer;
    private int srcIndex;


    public MtMcPull(IMultiChanPullEffect target, MtContext mtContext) {
        super(target, true, mtContext);
        if (!mtContext.isMultiCore())
            throw new IllegalStateException("MtScPull not supported when only one CPU core is used");
    }

    public boolean canWriteFasterThanRead() {
        return getBaseTarget().canWriteFasterThanRead();
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        int capacity = mtContext.getSerialRunningMaxSkew();
        queue = new ClockedAsynchronousQueue<float[][]>(capacity);
        curBuffer = null;
        srcIndex = 0;
        producerClock.set(0);
        consumerClock = 0;
        length = -1;
        nbChans = -1;
        if (getBaseTarget() instanceof IFixedChunkLength)
            length = ((IFixedChunkLength) getBaseTarget()).getFixedChunkLength();
        active = true;
        started = false;
        taskSources = new SourceTask[super.getInputPoolSize() + 1];
        for (int i = 0; i < taskSources.length; i++)
            taskSources[i] = new SourceTask();
        taskIndex = 0;
    }

    public int process(IMultiChanAudioSource source, float[][] output) {
        this.source = source;
        if (!started && active) {
            // Startup
            started = true;
            // Start nbChans if not yet set
            if (nbChans <= 0)
                nbChans = output.length;
            // Set length if not set yet
            if (length <= 0)
                length = output[0].length;
            // Create working buffers
            this.buffers = new float[taskSources.length][nbChans][length];
            // Add a threadpool factory to submit tasks that process and enqueue audio chunks
            threadPool.addTaskFactory(new Task() {

                @Override
                public void run() {
                    if (active)
                        submitNextTask();
                    else
                        threadPool.removeSelfTaskFactory();
                }
            });
        }
        int dstIndex = 0;
        // Poll produced buffers until output is filled
        while (dstIndex < output[0].length) {
            if (curBuffer == null || srcIndex >= curBuffer[0].length) {
                if (curBuffer != null && curBuffer[0].length < length) { // EOF reached
                    // Drain produced empty buffers
                    while (consumerClock < producerClock.get()) {
                        float[][] end = queue.take(consumerClock++);
                        assert end[0].length == 0 : "Buffer of length " + end[0].length + " received after EOF";
                    }
                    break; // This was the last buffer
                }
                // Get next buffer
                curBuffer = queue.take(consumerClock++);
                srcIndex = 0;
            }
            // Copy as much as possible from current buffer to output
            int amount = Math.min(output[0].length - dstIndex, curBuffer[0].length - srcIndex);
            if (amount > 0) {
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(curBuffer[chan], srcIndex, output[chan], dstIndex, amount);
            }
            srcIndex += amount;
            dstIndex += amount;
        }
        return dstIndex;
    }

    public AudioStreamFormat getAudioFormat() {
        return source.getAudioFormat();
    }

    /**
     * Submit the next task. Each submitted task process and produce one audio chunk, and enqueue it
     * into the queue.
     */
    private void submitNextTask() {
        SourceTask task = taskSources[taskIndex];
        float[][] buffer = buffers[taskIndex];
        task.init(producerClock.getAndIncrement(), buffer);
        taskIndex = (taskIndex + 1) % taskSources.length;
        threadPool.submitAsync(task);
    }

    /**
     * Task that process one audio chunk and enqueue the result
     */
    class SourceTask extends Task {

        private long taskClock;
        private float[][] output;


        void init(long taskClock, float[][] buffer) {
            this.taskClock = taskClock;
            this.output = buffer;
        }

        @Override
        public void run() {
            // Initialize serial sections' clock
            for (ISerialSectionExt serialSection : serialSections) {
                serialSection.setActive(true);
                serialSection.setClock(taskClock);
            }

            if (active) {
                // Produce next block
                // Get plugin from the pool
                IMultiChanPullEffect plugin = MtMcPull.super.getProcessingTarget(Thread.currentThread());
                // Process next block
                int amount = plugin.process(MtMcPull.this, output);
                if (amount < output[0].length) {
                    // This is the last block. Truncate the result
                    // Note that the source (MtScPull.this) has already set active to false at this time
                    float[][] tmp = output;
                    output = new float[nbChans][amount];
                    for (int chan = 0; chan < nbChans; chan++)
                        System.arraycopy(tmp[chan], 0, output[chan], 0, amount);
                }

                // Enqueue result
                queue.put(taskClock, output);

                // Update progress
                framesCounter.addAndGet(amount);
            } else {
                // Submit empty buffer to prevent queue deadlock if other threads are still concurrently producing chunks before EOF
                queue.put(taskClock, new float[nbChans][0]);
            }

            /*
             * Sync. Why? to ensure a slow task won't get later than more than ThreadPool.getPoolSize()
             * other threads. This is part of the contract and must be true at any time of the processing.
             * (As an example, StftSynthesizer#overlapAdd(), if used as the last processing step, may fail without
             * this sync by reading buffers than have already been written with subsequent data).
             */
            writeSection.sync();

            for (ISerialSectionExt serialSection : serialSections)
                serialSection.setActive(false);
        }
    }

    @Override
    public void stopProcessing() {
        active = false;
        queue.dispose();
        super.stopProcessing();
        source = null;
        queue = null;
        curBuffer = null;
        taskSources = null;
        buffers = null;
    }

    // IMultiChanAudioSource

    public int readSamples(float[][] target) {
        boolean success = false;
        while (!success) {
            success = readSection.tryEnterSerialSection(TimeUnit.MILLISECONDS.toNanos(200));
            if (!active)
                return 0;
        }
        try {
            if (!active)
                return 0;
            int result = source.readSamples(target);
            if (result < target[0].length)
                active = false; // This is the last block
            return result;
        } finally {
            readSection.leaveSerialSection();
        }
    }

    public void close() {
        source.close();
    }

}
