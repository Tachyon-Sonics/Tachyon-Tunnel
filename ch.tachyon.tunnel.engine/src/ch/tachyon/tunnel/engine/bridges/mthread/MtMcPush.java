package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.bridges.mthread.serial.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.*;

public class MtMcPush extends MtBase<IMultiChanPushEffect> implements IMultiChanPushEffect {

    private float[][][] buffers;
    private int bufferIndex;

    private TaskSink[] taskSinks;
    private int taskIndex;
    private long clock;


    public MtMcPush(IMultiChanPushEffect target, MtContext mtContext) {
        super(target, false, mtContext);
    }

    public boolean canWriteFasterThanRead() {
        return getBaseTarget().canWriteFasterThanRead();
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        this.buffers = null;
        taskSinks = new TaskSink[super.getInputPoolSize() + 1]; // +1 for the one being initialized
        for (int i = 0; i < taskSinks.length; i++) {
            taskSinks[i] = new TaskSink();
        }
        clock = 0;
    }

    public void process(final float[][] input0, final int length, final IMultiChanAudioSink sink) {
        int nbChans = input0.length;
        int nbSamples = input0[0].length;
        if (buffers == null)
            buffers = new float[getInputPoolSize() + 1][][];
        // Copy into a working buffer (to avoid simulatenous access by multiple threads)
        float[][] input = getNextBuffer(nbChans, nbSamples);
        for (int c = 0; c < nbChans; c++)
            System.arraycopy(input0[c], 0, input[c], 0, nbSamples);
        // Create and submit a Task to the thread pool
        submitChunk(sink, input, (length < nbSamples), length);
    }

    private float[][] getNextBuffer(int nbChans, int nbSamples) {
        float[][] input = buffers[bufferIndex];
        if (input == null || input.length != nbChans || input[0].length != nbSamples) {
            input = new float[nbChans][nbSamples];
            buffers[bufferIndex] = input;
        }
        bufferIndex = (bufferIndex + 1) % buffers.length;
        return input;
    }

    private void submitChunk(final IMultiChanAudioSink sink, float[][] input, boolean isLast, int chunkLength) {
        TaskSink task = taskSinks[taskIndex];
        taskIndex = (taskIndex + 1) % taskSinks.length;
        task.init(input, chunkLength, clock++, sink);
        if (serialize)
            task.run();
        else {
            started();
            threadPool.submitFragment(task);

            if (isLast)
                waitCompletion();
        }
    }

    // Completion

    private AtomicInteger nbTasks = new AtomicInteger();
    private volatile Thread completionWaiter = null;

    void started() {
        nbTasks.incrementAndGet();
    }

    void completed() {
        int newValue = nbTasks.decrementAndGet();
        Thread waiter = completionWaiter;
        if (newValue == 0 && waiter != null)
            LockSupport.unpark(waiter);
    }

    private void waitCompletion() {
        completionWaiter = Thread.currentThread();
        while (nbTasks.get() > 0)
            LockSupport.park();
        completionWaiter = null;
    }

    @Override
    public void stopProcessing() {
        waitCompletion();
        super.stopProcessing();
        Arrays.fill(taskSinks, null);
    }

    class TaskSink extends Task implements IMultiChanAudioSink {

        private float[][] input;
        private int length;
        private long clock;
        private IMultiChanAudioSink sink;

        private final List<float[][]> outputs = new ArrayList<float[][]>();


        /**
         * @param input the input audio block to process
         * @param length the length of the input audio block
         * @param clock the logical clock for the serial section
         * @param sink the sink to which to write the result
         */
        public void init(float[][] input, int length, long clock, IMultiChanAudioSink sink) {
            this.input = input;
            this.length = length;
            this.clock = clock;
            this.sink = sink;
            this.outputs.clear();
        }

        @Override
        public void run() {
            // Initialize serial sections' clock
            for (ISerialSectionExt serialSection : serialSections) {
                serialSection.setActive(true);
                serialSection.setClock(clock);
            }
            // Get a plugin from the pool
            IMultiChanPushEffect plugin = MtMcPush.super.getProcessingTarget(Thread.currentThread());
            if (plugin == null) {
                assert serialize;
                plugin = getBaseTarget();
            }
            // Process, accumulating output samples in 'outputs'
            plugin.process(input, length, this);

            // Write output samples
            // Note that we must enter the section even if no samples have been produced to advance the logical clock
            writeSection.enterSerialSection();
            try {
                for (float[][] output : outputs) {
                    sink.writeSamples(output);
                }
            } finally {
                writeSection.leaveSerialSection();
            }

            // Update progress
            framesCounter.addAndGet(length);

            // Notify completion if necessary
            completed();

            for (ISerialSectionExt serialSection : serialSections)
                serialSection.setActive(false);
        }

        public void writeSamples(float[][] samples) {
            // Cumulate the produced output samples so that they can be written all at once in the
            // serial section at the end of the input block processing
            outputs.add(samples);
        }

        public void close() {
        }

    }

}
