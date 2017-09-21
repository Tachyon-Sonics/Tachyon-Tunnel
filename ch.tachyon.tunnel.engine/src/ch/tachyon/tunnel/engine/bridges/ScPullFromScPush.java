package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.IPushEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.utils.*;

/**
 * Wrap a plugin implementing {@link IPushEffect} into the {@link ISingleChanPullEffect} host interface.
 * <p>
 * The implementation uses two threads (the main one and an auxiliary one), and a shared FIFO samples queue.
 * <ul>
 * <li>The auxiliary thread read samples from the {@link ISingleChanAudioSource}, and process them using
 * the {@link IPushEffect} plugin using a custom {@link ISingleChanAudioSink} implementation that pushes the
 * produced samples on the shared samples queue.
 * <li>The {@link #process(ISingleChanAudioSource, float[])} method, called by the main thread,
 * just poll samples from the shared samples queue to fill the target array. It also copies the
 * {@link ISingleChanAudioSource} to a field so that the auxiliary thread can use it.
 * </ul>
 */
public class ScPullFromScPush extends PluginToHostWrapper<IPushEffect> implements ISingleChanPullEffect, Runnable {

    private final ISingleChanAudioSink sink = new AudioSink();

    private int inputLength;
    private int maxBlockLength;
    private int queueCapacity;

    private int headOutToSkip;
    private int tailInToPad;

    private FloatQueue queue;
    private final Object lock = new Monitor("ScPullFromScPush");
    private ISingleChanAudioSource source;
    private Thread puller;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IPushEffect) processingTarget, ISingleChanPullEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        // Reset queue
        queue = null;
        source = null;
        // Init capacities
        inputLength = getActualChunkLength();
        maxBlockLength = getActualMaxChunkLength(info);
        queueCapacity = inputLength + maxBlockLength;
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
    public int process(ISingleChanAudioSource source, float[] output) {
        beginIfWorth();
        if (output.length > maxBlockLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        synchronized (lock) {
            this.source = source;
            if (queue == null) {
                // Startup
                queue = new FloatQueue(queueCapacity);
                lock.notify();
            }
            // Wait for data to be produced
            while (queue.getSize() < output.length && puller != null)
                waitLock();
            // If puller==null, EOF has been reached
            int amount = (puller != null ? output.length : Math.min(output.length, queue.getSize()));
            if (amount > 0) {
                queue.popArray(output, 0, amount);
                lock.notify();
            }
            return amount;
        }
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        queue = null;
        source = null;
        puller = null;
    }

    /**
     * Loop that process samples from the source and push them to the queue
     */
    public void run() {
        // Wait for startup
        synchronized (lock) {
            while (queue == null)
                waitLock();
        }
        assert source != null;
        boolean eof = false;
        boolean lastBuffer = false;
        float[] buffer = new float[inputLength];
        // Processing loop
        while (!lastBuffer) {
            // Read data from the source
            int amount = (eof ? 0 : source.readSamples(buffer));
            float[] input = wrapInput(buffer);
            if (amount < buffer.length) {
                eof = true; // Stop reading from the source
                if (tailInToPad > 0) {
                    int padAmount = Math.min(tailInToPad, buffer.length - amount);
                    tailInToPad -= padAmount;
                    Arrays.fill(input, amount, buffer.length, 0.0f);
                    amount += padAmount;
                    if (amount < buffer.length)
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
    class AudioSink implements ISingleChanAudioSink {

        public void writeSamples(float[] samples) {
            assert isSane(samples, false);
            int offset = 0;
            while (offset < samples.length) {
                int amount = Math.min(samples.length - offset, maxBlockLength);
                synchronized (lock) {
                    if (headOutToSkip >= amount)
                        headOutToSkip -= amount;
                    else if (headOutToSkip > 0) {
                        float[] head = new float[amount - headOutToSkip];
                        System.arraycopy(samples, offset + headOutToSkip, head, 0, head.length);
                        push(head, 0, head.length);
                        headOutToSkip = 0;
                    } else {
                        push(samples, offset, amount);
                    }
                    lock.notify();
                }
                offset += amount;
            }
        }

        private void push(float[] data, int offset, int length) {
            while (queue.getSize() + length > queueCapacity)
                waitLock();
            queue.pushArray(data, offset, length);
        }

        public void close() {

        }

    }

    void waitLock() {
        try {
            lock.wait();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

}
