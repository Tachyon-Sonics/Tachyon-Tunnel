package ch.tachyon.tunnel.engine.bridges.chan.expand;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;


public class McPushFromScPush extends McFromScBase<IPushEffect, ISingleChanPushEffect> implements IPushEffect {
    
    private List<Queue<float[]>> outputs; // chan x frame
    private int[] offsets; // chan
    private ProcessOnChanTask[][] tasks;
    private CollectingSink[] sinks;
    private volatile IMultiChanAudioSink mcSink;
    private final Object processLock = new Monitor("McPushFromScPush#process");
    private final AtomicReference<RuntimeException> exRef = new AtomicReference<>();
    private Queue<float[]> freePool;
    
    private transient float[][] nextBlock;
    private transient float[][] dequeued;
    
    
    @Override
    public Class<? extends IEffect> getInterface() {
        return IPushEffect.class;
    }

    public boolean canWriteFasterThanRead() {
        return descTarget.canWriteFasterThanRead();
    }
    
    class CollectingSink implements ISingleChanAudioSink {
        
        private final int chan;

        public CollectingSink(int chan) {
            this.chan = chan;
        }

        public void writeSamples(float[] samples) {
            float[] copy = freePool.poll();
            if (copy == null || copy.length != samples.length)
                copy = new float[samples.length];
            System.arraycopy(samples, 0, copy, 0, samples.length);
            synchronized (processLock) { // Sync against multiple channels
                Queue<float[]> queue = outputs.get(chan);
                queue.add(copy);
                try {
                    transmitNewlyAvailableFrames();
                } catch (RuntimeException ex) {
                    exRef.compareAndSet(null, ex);
                }
            }
        }

        public void close() {
        }
        
    }

    void transmitNewlyAvailableFrames() {
        int available;
        do {
            // Get the maximum amount of data available in all channels
            available = Integer.MAX_VALUE;
            for (int chan = 0; chan < nbChans; chan++) {
                Queue<float[]> chanQueue = outputs.get(chan);
                if (chanQueue.isEmpty()) {
                    available = 0;
                } else {
                    float[] current = chanQueue.peek();
                    available = Math.min(available, current.length - offsets[chan]);
                }
            }
            // Send the available amount
            if (available > 0) {
                for (int chan = 0; chan < nbChans; chan++) {
                    Queue<float[]> chanQueue = outputs.get(chan);
                    float[] current = chanQueue.peek();
                    dequeued[chan] = null;
                    if (offsets[chan] == 0 && current.length == available) // prevent alloc/copy if possible
                        nextBlock[chan] = current;
                    else {
                        nextBlock[chan] = new float[available];
                        System.arraycopy(current, offsets[chan], nextBlock[chan], 0, available);
                    }
                    offsets[chan] += available;
                    if (offsets[chan] >= current.length) {
                        assert offsets[chan] == current.length;
                        dequeued[chan] = chanQueue.remove();
                        offsets[chan] = 0;
                    }
                }
                super.postProcessMidSide(nextBlock);
                mcSink.writeSamples(nextBlock);
                for (int chan = 0; chan < nbChans; chan++) {
                    if (dequeued[chan] != null && dequeued[chan] != nextBlock[chan]) // WAIT review with granularize, mirror and ambiance (remove second cond?)
                        freePool.add(dequeued[chan]);
                }
            }
        } while (available > 0);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        sinks = new CollectingSink[nbChans];
        tasks = new ProcessOnChanTask[2][nbChans];
        outputs = new ArrayList<>(nbChans);
        freePool = new ConcurrentLinkedQueue<>();
        offsets = new int[nbChans];
        for (int chan = 0; chan < nbChans; chan++) {
            sinks[chan] = new CollectingSink(chan);
            tasks[0][chan] = new ProcessOnChanTask(chan);
            tasks[1][chan] = new ProcessOnChanTask(chan);
            Queue<float[]> emptyList = new LinkedList<>();
            outputs.add(emptyList);
        }
        if (pool != null)
            work = new float[2][][];
        nextBlock = new float[nbChans][];
        dequeued = new float[nbChans][];
    }
    
    
    private transient float[][][] work; // Buffers for double buffering
    private transient int curBuffer = 0; // Current buffer, 0 or 1
    private transient CountDownLatch latch; // Latch for waiting previous buffer
    
    public void process(float[][] input, int length, IMultiChanAudioSink mcSink) {
        if (input.length != nbChans)
            throw new IllegalArgumentException("Wrong number of channels. Found input.length = " + input.length + ", excpected: " + nbChans);
        assert targets.size() == nbChans;
        this.mcSink = mcSink;
        super.preProcessMidSide(input);
        
        if (pool != null) {
            // Create/update buffers for double buffering
            if (work[curBuffer] == null || work[curBuffer].length != input.length || work[curBuffer][0].length != input[0].length)
                work[curBuffer] = new float[input.length][input[0].length];
            
            // Prepare tasks and copy buffers
            for (int chan = 0; chan < nbChans; chan++) {
                System.arraycopy(input[chan], 0, work[curBuffer][chan], 0, length);       
                tasks[curBuffer][chan].setup(work[curBuffer][chan], length);
            }
            
            // Wait previous buffer
            if (latch != null)
                await(latch);
            
            // Submit new buffer
            latch = pool.submit(tasks[curBuffer]);
            if (length < input[0].length) { // This is the last buffer: flush
                await(latch);
                latch = null;
            }
            
            // Switch buffer
            curBuffer = 1 - curBuffer;
            
            // Propagate any exception
            RuntimeException ex = exRef.getAndSet(null);
            if (ex != null)
                throw ex;
        } else {
            // Not multithreaded. Do not use double buffering
            for (int chan = 0; chan < nbChans; chan++) {
                targets.get(chan).process(input[chan], length, sinks[chan]);
            }
        }
    }
    
    static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    class ProcessOnChanTask extends Task {
        
        private final int chan;
        private float[] input;
        private int length;

        public ProcessOnChanTask(int chan) {
            this.chan = chan;
        }
        
        void setup(float[] input, int length) {
            this.input = input;
            this.length = length;
        }

        @Override
        public void run() {
            targets.get(chan).process(input, length, sinks[chan]);
        }
        
    }
    
    @Override
    public void stopProcessing() {
        if (latch != null)
            await(latch);
        super.stopProcessing();
        outputs = null;
        tasks = null;
        sinks = null;
        work = null;
        curBuffer = 0;
        latch = null;
        freePool = null;
        nextBlock = null;
        dequeued = null;
    }

}
