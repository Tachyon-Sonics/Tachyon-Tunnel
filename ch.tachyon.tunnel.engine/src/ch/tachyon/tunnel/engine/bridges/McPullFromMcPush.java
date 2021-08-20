package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.IPullEffect;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class McPullFromMcPush extends PluginToHostWrapper<IMultiChanPushEffect> implements IPullEffect, Runnable {

    private final IMultiChanAudioSink sink = new AudioSink();

    private int inputLength;
    private int maxBlockLength;
    private int queueCapacity;
    private int nbChans;

    private int headOutToSkip;
    private int tailInToPad;

    private FloatQueue[] queues;
    private final Object lock = new Monitor("McPullFromMcPush");
    private IMultiChanAudioSource source;
    private Thread puller;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanPushEffect) processingTarget, IPullEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        nbChans = info.getNumberOfChannels();
        // Reset queue
        queues = null;
        source = null;
        // Init capacities
        inputLength = getActualChunkLength();
        maxBlockLength = getActualMaxChunkLength(info);
        queueCapacity = maxBlockLength * 2;
        // Init latency compensation param
        headOutToSkip = super.getLatency(IoDirection.OUTPUT);
        tailInToPad = super.getLatency(IoDirection.INPUT);
        // Start puller thread
        // Start pusher thread
        puller = new Thread(this);
        puller.setName(getName() + "-Puller");
        puller.start();
    }

    public boolean canWriteFasterThanRead() {
        return processingTarget.canWriteFasterThanRead();
    }

    /**
     * Process by polling produced data from the queue
     */
    public int process(IMultiChanAudioSource source, float[][] output) {
        beginIfWorth();
        if (output[0].length > maxBlockLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        synchronized (lock) {
            this.source = source;
            if (queues == null) {
                // Startup
                queues = new FloatQueue[nbChans];
                for (int chan = 0; chan < nbChans; chan++)
                    queues[chan] = new FloatQueue(queueCapacity);
                lock.notify();
            }
            // Wait for data to be produced
            while (queues[0].getSize() < output[0].length && puller != null)
                waitLock();
            // If puller==null, EOF has been reached
            int amount = (puller != null ? output[0].length : Math.min(output[0].length, queues[0].getSize()));
            if (amount > 0) {
                for (int chan = 0; chan < nbChans; chan++)
                    queues[chan].popArray(output[chan], 0, amount);
                lock.notify();
            }
            return amount;
        }
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        queues = null;
        source = null;
        puller = null;
    }

    /**
     * Loop that process samples from the source and push them to the queue
     */
    public void run() {
        // Wait for startup
        synchronized (lock) {
            while (queues == null)
                waitLock();
        }
        assert source != null;
        boolean eof = false;
        boolean lastBuffer = false;
        float[][] buffer = new float[nbChans][inputLength];
        // Processing loop
        while (!lastBuffer) {
            // Read data from the source
            int amount = (eof ? 0 : source.readSamples(buffer));
            float[][] input = wrapInput(buffer);
            if (amount < buffer[0].length) {
                eof = true; // Stop reading from the source
                if (tailInToPad > 0) {
                    int padAmount = Math.min(tailInToPad, buffer[0].length - amount);
                    tailInToPad -= padAmount;
                    for (int chan = 0; chan < nbChans; chan++)
                        Arrays.fill(input[chan], amount, buffer[chan].length, 0.0f);
                    amount += padAmount;
                    if (amount < buffer[0].length)
                        lastBuffer = true;
                } else
                    lastBuffer = true;
            }
            processingTarget.process(input, amount, sink);
            super.processed(amount);
        }
        synchronized (lock) {
            // Notify we are finished by clearing puller
            puller = null;
            lock.notify();
        }
    }

    /**
     * Sink that push samples to the queue
     */
    class AudioSink implements IMultiChanAudioSink {

        public void writeSamples(float[][] samples) {
            assert isSane(samples, false);
            int offset = 0;
            while (offset < samples[0].length) {
                int amount = Math.min(samples[0].length - offset, maxBlockLength);
                synchronized (lock) {
                    if (headOutToSkip >= amount)
                        headOutToSkip -= amount;
                    else if (headOutToSkip > 0) {
                        float[][] head = new float[nbChans][amount - headOutToSkip];
                        for (int chan = 0; chan < nbChans; chan++)
                            System.arraycopy(samples[chan], offset + headOutToSkip, head[chan], 0, head[0].length);
                        push(head, 0, head[0].length);
                        headOutToSkip = 0;
                    } else {
                        push(samples, offset, amount);
                    }
                    lock.notify();
                }
                offset += amount;
            }
        }

        private void push(float[][] data, int offset, int length) {
            while (queues[0].getSize() + length > queueCapacity)
                waitLock();
            for (int chan = 0; chan < nbChans; chan++)
                queues[chan].pushArray(data[chan], offset, length);
        }

        public void close() {

        }

    }

    void waitLock() {
        try {
            lock.wait();
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

}
