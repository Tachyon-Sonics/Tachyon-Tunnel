package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.IPullEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link IPullEffect} interface into the host {@link ISingleChanPushEffect}
 * interface.
 * <p>
 * The implementation uses an auxiliary thread and a shared FIFO sample queue.
 * <ul>
 * <li>The {@link #process(float[], int, ISingleChanAudioSink)} method just pushes the input
 * samples on the shared sample queue, and copies the actual {@link ISingleChanAudioSink} to a field
 * so that the auxiliary thread can use it.
 * <li>The auxiliary thread does the actual processing using the {@link IPullEffect} plugins, by passing
 * a custom {@link ISingleChanAudioSource} implementation that reads samples from the shared sample queue.
 * It then writes the resulting output samples array to the actual {@link ISingleChanAudioSink}.
 * </ul>
 */
public class ScPushFromScPull extends PluginToHostWrapper<IPullEffect> implements ISingleChanPushEffect,
        Runnable {

    private final ISingleChanAudioSource source = new AudioSource();
    private final Object lock = new Monitor("ScPushFromScPull");

    private int maxInputLength;
    private int outputLength;
    private int queueCapacity;

    private int headOutToSkip;
    private int tailInToPad;

    private FloatQueue queue;
    private ISingleChanAudioSink sink;
    private Thread pusher;
    private long nbPushed; // Number of valid pushed samples
    private long nbPolled; // Number of polled samples (valid or not)


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IPullEffect) processingTarget, ISingleChanPushEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        // Reset queue
        queue = null;
        sink = null;
        nbPushed = 0;
        nbPolled = 0;
        // Init capacities
        outputLength = getPreferredChunkLength();
        maxInputLength = info.getMaxChunkLength(outputLength);
        if (outputLength < 0)
            outputLength = info.negociateFixedChunkLength(outputLength);
        if (outputLength < 0)
            outputLength = DEFAULT_MAX_LENGTH;
        queueCapacity = maxInputLength + Math.max(outputLength, maxInputLength);
        // Init latency compensation param
        headOutToSkip = super.getLatency(IoDirection.OUTPUT);
        tailInToPad = super.getLatency(IoDirection.INPUT);
        // Start pusher thread
        pusher = new Thread(this);
        pusher.setName(getName() + "-Pusher");
        pusher.start();
    }

    public boolean canWriteFasterThanRead() {
        return processingTarget.canWriteFasterThanRead();
    }

    /**
     * Process by just pushing input samples to the queue.
     * {@link #run()} will poll them and do the actual processing.
     */
    public void process(float[] samples, int length, ISingleChanAudioSink sink) {
        beginIfWorth();
        if (length > maxInputLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
            		"by the IProcessingInfo object passed to startProcessing()");
        assert super.isSane(samples, true);
        synchronized (lock) {
            if (queue == null)
                queue = new FloatQueue(queueCapacity);
            while (queue.getSize() + length > queueCapacity)
                waitLock();
            queue.pushArray(samples, length);
            this.sink = sink;
            nbPushed += length;
            lock.notify();
        }
        if (length < samples.length)
            finishProcessing(sink);
    }

    private void finishProcessing(ISingleChanAudioSink sink) {
        float[] zero = new float[Math.min(maxInputLength, outputLength)];
        synchronized (lock) {
            while (pusher != null) {
                // Push zeroes until the last output block can be produced
                while (queue.getSize() + zero.length > queueCapacity) {
                    waitLock();
                    if (pusher == null)
                        return; // Finished
                }
                queue.pushArray(zero);
                if (tailInToPad > 0) {
                    // Compensate for latency
                    int padAmount = Math.min(zero.length, tailInToPad);
                    nbPushed += padAmount; // Add tailInToPad zero samples as valid
                    tailInToPad -= padAmount;
                }
                lock.notify();
            }
        }
    }

    @Override
    public void stopProcessing() {
        finishProcessing(new ISingleChanAudioSink() {

            public void writeSamples(float[] samples) {
                // Absorb
            }

            public void close() {
            }
        });
        super.stopProcessing();
        queue = null;
        sink = null;
        pusher = null;
    }

    /**
     * Loop that polls samples from the queue, process them and send them to the sink.
     * Polling from the queue is done indirectly by the 'source' callback of type AudioSource
     * passed to process().
     */
    public void run() {
        // Wait for startup
        synchronized (lock) {
            while (nbPushed == 0)
                waitLock();
        }
        float[] buffer = new float[outputLength];
        // Processing loop
        while (true) {
            // Pull and process
            int count = processingTarget.process(source, buffer);
            // Write result to the sink
            if (count == buffer.length)
                writeSamples(sink, buffer);
            else if (count > 0) {
                float[] temp = new float[count];
                System.arraycopy(buffer, 0, temp, 0, count);
                writeSamples(sink, temp);
            }
            if (count < buffer.length) {
                synchronized (lock) {
                    // EOF reached. Notify that we are finished
                    pusher = null;
                    lock.notify();
                    break;
                }
            }
        }
    }

    /**
     * Write output samples, compensating for the output head latency
     */
    public void writeSamples(ISingleChanAudioSink sink, float[] samples) {
        assert super.isSane(samples, false);
        int amount = samples.length;
        if (headOutToSkip >= amount)
            headOutToSkip -= amount;
        else if (headOutToSkip > 0) {
            float[] head = new float[amount - headOutToSkip];
            System.arraycopy(samples, headOutToSkip, head, 0, head.length);
            sink.writeSamples(head);
            headOutToSkip = 0;
        } else
            sink.writeSamples(samples);
    }
    /**
     * {@link ISingleChanAudioSource} implementation that polls from the shared queue.
     * Data is pushed to the queue by #process
     */
    class AudioSource implements ISingleChanAudioSource {

        public int readSamples(float[] target) {
            synchronized (lock) {
                int offset = 0;
                int remaining = target.length;
                while (remaining > 0) {
                    int amount = Math.min(maxInputLength, remaining);
                    // Wait until enough data has been pushed
                    while (queue.getSize() < amount)
                        waitLock();
                    // Poll
                    queue.popArray(target, offset, amount);
                    nbPolled += amount;
                    lock.notify();
                    remaining -= amount;
                    offset += amount;
                }

                int result = target.length;
                if (nbPolled > nbPushed)
                    result = Math.max(0, result - (int) (nbPolled - nbPushed));
                ScPushFromScPull.super.processed(result);
                return result;
            }
        }

        public AudioStreamFormat getAudioFormat() {
            return ProcessingInfo.toAudioFormat(ScPushFromScPull.super.getHostInfo());
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
