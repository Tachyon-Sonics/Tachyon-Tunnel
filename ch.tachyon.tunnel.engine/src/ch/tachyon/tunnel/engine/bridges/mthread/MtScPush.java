package ch.tachyon.tunnel.engine.bridges.mthread;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.bridges.mthread.serial.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.plugin.*;

/**
 * Wrap an {@link IPushEffect} into a parallel version.
 */
public class MtScPush extends MtBase<IPushEffect> implements IPushEffect {

    private float[][] buffers;
    private int bufferIndex;

    private TaskSink[] taskSinks;
    private int taskIndex;
    private long clock;


    public MtScPush(IPushEffect target, MtContext mtContext) {
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
        for (int i = 0; i < taskSinks.length; i++)
            taskSinks[i] = new TaskSink();
        clock = 0;
    }

    public void process(final float[] input0, final int length, final ISingleChanAudioSink sink) {
        if (buffers == null)
            buffers = new float[getInputPoolSize() + 1][];
        // Copy into a working buffer (to avoid simulatenous access by multiple threads)
        float[] input = getNextBuffer(input0.length);
        assert input.length >= input0.length;
        System.arraycopy(input0, 0, input, 0, input0.length);
        // Create and submit a Task to the thread pool
        submitChunk(sink, input0.length, input, (length < input0.length), length + historySize, historySize);
    }

    private float[] getNextBuffer(int length) {
        float[] input = buffers[bufferIndex];
        int bufSize = (pluginFixedSize > 0 ? pluginFixedSize : length + historySize);
        if (input == null || input.length != bufSize) {
            input = new float[bufSize];
            buffers[bufferIndex] = input;
        }
        bufferIndex = (bufferIndex + 1) % buffers.length;
        return input;
    }

    private void submitChunk(final ISingleChanAudioSink sink, int baseInputLength, float[] input, boolean isLast, int chunkLength, int crop) {
        TaskSink task = taskSinks[taskIndex];
        taskIndex = (taskIndex + 1) % taskSinks.length;
        task.init(baseInputLength, input, chunkLength, crop, clock++, sink);
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

    class TaskSink extends Task implements ISingleChanAudioSink {

        private int baseInputLength;
        private float[] input;
        private int length;
        private long clock;
        private int historySizeToCrop;
        private ISingleChanAudioSink sink;

        private final List<float[]> outputs = new ArrayList<float[]>();


        /**
         * @param input the input audio block to process
         * @param length the length of the input audio block
         * @param clock the logical clock for the serial section
         * @param sink the sink to which to write the result
         */
        public void init(int baseInputLength, float[] input, int length, int historySizeToCrop, long clock, ISingleChanAudioSink sink) {
            this.baseInputLength = baseInputLength;
            this.input = input;
            this.length = length;
            this.clock = clock;
            this.sink = sink;
            this.outputs.clear();
            this.historySizeToCrop = historySizeToCrop;
        }

        @Override
        public void run() {
            // Initialize serial sections' clock
            for (ISerialSectionExt serialSection : serialSections) {
                serialSection.setActive(true);
                serialSection.setClock(clock);
            }
            // Handle history
            if (historySizeToCrop > 0) {
                historyBuffer.write(input, baseInputLength, clock * baseInputLength);
                historySection.sync();
                historyBuffer.read(-historySizeToCrop, input, clock * baseInputLength);
            }
            // Get a plugin from the pool
            IPushEffect plugin = MtScPush.super.getProcessingTarget(Thread.currentThread());
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
                for (float[] output : outputs) {
                    if (historySizeToCrop > output.length) {
                        historySizeToCrop-= output.length;
                    } else if (historySizeToCrop > 0) {
                        int croppedLength = output.length - historySizeToCrop;
                        float[] cropped = new float[croppedLength];
                        System.arraycopy(output, output.length - croppedLength, cropped, 0, croppedLength);
                        sink.writeSamples(cropped);
                    } else {
                        sink.writeSamples(output);
                    }
                }
            } finally {
                writeSection.leaveSerialSection();
            }

            // Update progress
            framesCounter.addAndGet(length - historySizeToCrop);

            // Notify completion if necessary
            completed();

            for (ISerialSectionExt serialSection : serialSections)
                serialSection.setActive(false);
        }

        public void writeSamples(float[] samples) {
            // Cumulate the produced output samples so that they can be written all at once in the
            // serial section at the end of the input block processing
            outputs.add(samples);
        }

        public void close() {
        }

    }

}
