package ch.tachyon.tunnel.engine.bridges.chan;

import java.util.*;
import java.util.concurrent.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.utils.*;


public class McPullFromScPull extends McFromScBase<IPullEffect, ISingleChanPullEffect> implements IPullEffect {
    
    private List<Queue<float[]>> inputs; // chan x frame
    private int[] offsets; // chan
    private ProcessOneChanTask[][] tasks;
    private CollectingSource[] sources;
    private volatile IMultiChanAudioSource mcSource;
    private final Object processLock = new Monitor("McPullFromScPull#process");
    private Queue<float[]> freePool;
    
    private transient float[][] block;

    
    @Override
    public Class<? extends IEffect> getInterface() {
        return IPullEffect.class;
    }

    public boolean canWriteFasterThanRead() {
        return descTarget.canWriteFasterThanRead();
    }
    
    class CollectingSource implements ISingleChanAudioSource {
        
        private final AudioFormat format;
        private final int chan;

        public CollectingSource(AudioFormat format, int chan) {
            this.format = format;
            this.chan = chan;
        }

        public AudioFormat getAudioFormat() {
            return format;
        }

        public int readSamples(float[] target) {
            int remaining = target.length;
            int result = 0;
            synchronized (processLock) {
                while (remaining > 0) {
                    if (inputs.get(chan).isEmpty())
                        fetchNextInputBlock(remaining);
                    if (inputs.get(chan).isEmpty())
                        return result; // end of stream
                    float[] block = inputs.get(chan).peek();
                    int amount = Math.min(remaining, block.length - offsets[chan]);
                    System.arraycopy(block, offsets[chan], target, result, amount);
                    remaining -= amount;
                    result += amount;
                    offsets[chan] += amount;
                    if (offsets[chan] >= block.length) {
                        assert offsets[chan] == block.length;
                        float[] toFree = inputs.get(chan).remove();
                        freePool.add(toFree);
                        offsets[chan] = 0;
                    }
                }
            }
            return result;
        }

        public void close() {

        }
        
    }
        
    void fetchNextInputBlock(int suggestedLength) {
        for (int chan = 0; chan < nbChans; chan++) {
            float[] chanBlock = freePool.poll();
            if (chanBlock == null || chanBlock.length != suggestedLength)
                chanBlock = new float[suggestedLength];
            block[chan] = chanBlock;
        }
        int read = mcSource.readSamples(block);
        if (read == 0)
            return; // end of audio stream - dot not add an empty array to the queue
        if (read < suggestedLength) {
            float[][] tail = new float[nbChans][read];
            for (int chan = 0; chan < nbChans; chan++)
                System.arraycopy(block[chan], 0, tail[chan], 0, read);
            block = tail;
        }
        super.preProcessMidSide(block);
        for (int chan = 0; chan < nbChans; chan++)
            inputs.get(chan).add(block[chan]);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        this.sources = new CollectingSource[nbChans];
        this.tasks = new ProcessOneChanTask[2][nbChans];
        this.inputs = new ArrayList<Queue<float[]>>(nbChans);
        this.offsets = new int[nbChans];
        AudioFormat format = new AudioFormat(info.getSampleRate(), info.getQuantization(), nbChans, true, false);
        for (int chan = 0; chan < nbChans; chan++) {
            sources[chan] = new CollectingSource(format, chan);
            tasks[0][chan] = new ProcessOneChanTask(chan);
            tasks[1][chan] = new ProcessOneChanTask(chan);
            inputs.add(new LinkedList<float[]>());
        }
        if (pool != null)
            work = new float[2][][];
        freePool = new ConcurrentLinkedQueue<float[]>();
        block = new float[nbChans][];
    }

    
    private transient float[][][] work; // Buffers for double buffering
    private transient int curBuffer = 0; // Current buffer, 0 or 1
    private transient CountDownLatch latch; // Latch for waiting previous buffer
    
    public int process(IMultiChanAudioSource source, float[][] output) {
        if (output.length != nbChans)
            throw new IllegalArgumentException("Wrong number of channels. Found output.length = " + output.length + ", expected: " + nbChans);
        assert targets.size() == nbChans;
        this.mcSource = source;
        
        int result = 0;
        if (pool != null) {
            // First time: launch two buffers. Then 1 buffer at a time
            int nbBuffers = (latch == null ? 2 : 1);
            
            for (int k = 0; k < nbBuffers; k++) {
                // Create/update buffers for double buffering
                if (work[curBuffer] == null || work[curBuffer].length != output.length || work[curBuffer][0].length != output[0].length)
                    work[curBuffer] = new float[output.length][output[0].length];
                
                // Prepare tasks
                for (int chan = 0; chan < nbChans; chan++)
                    tasks[curBuffer][chan].setup(work[curBuffer][chan]);
                
                // Wait previous buffer
                if (latch != null)
                    await(latch);
                
                // Submit new buffer
                latch = pool.submit(tasks[curBuffer]);
    
                // Switch buffer
                curBuffer = 1 - curBuffer;
            }
            
            // Collect previous result
            for (int chan = 0; chan < nbChans; chan++) {
                int r = tasks[curBuffer][chan].getResult();
                if (chan == 0)
                    result = r;
                else if (result != r)
                    throw new IllegalStateException("Plugin " + descTarget.getClassName() + " returned an inconsistent number of samples accross channels."
                            + " Channel 0 returned " + result + " samples and channel " + chan + " returned " + r + " samples");
            }
            
            // Copy result
            if (result > 0) {
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(work[curBuffer][chan], 0, output[chan], 0, result);
            }
        } else {
            // Not multithreaded. Do not use double buffering
            for (int chan = 0; chan < nbChans; chan++) {
                int r = targets.get(chan).process(sources[chan], output[chan]);
                if (chan == 0)
                    result = r;
                else if (result != r)
                    throw new IllegalStateException("Plugin " + descTarget.getClassName() + " returned an inconsistent number of samples accross channels."
                            + " Channel 0 returned " + result + " samples and channel " + chan + " returned " + r + " samples");
            }
        }
        
        super.postProcessMidSide(output);
        return result;
    }
    
    static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    class ProcessOneChanTask extends Task {
        
        private final int chan;
        private float[] output;
        private int result;
        
        public ProcessOneChanTask(int chan) {
            this.chan = chan;
        }
        
        void setup(float[] output) {
            this.output = output;
        }
        
        public int getResult() {
            return result;
        }
        
        @Override
        public void run() {
            result = targets.get(chan).process(sources[chan], output);
        }
        
    }

    @Override
    public void stopProcessing() {
        if (latch != null)
            await(latch);
        super.stopProcessing();
        inputs = null;
        sources = null;
        tasks = null;
        offsets = null;
        work = null;
        curBuffer = 0;
        latch = null;
        freePool = null;
        block = null;
    }

}
