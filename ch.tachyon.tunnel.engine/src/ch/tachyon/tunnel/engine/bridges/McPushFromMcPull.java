package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class McPushFromMcPull extends PluginToHostWrapper<IMultiChanPullEffect> implements ch.tachyon.tunnel.host.effect.IPushEffect,
        Runnable {

    private final IMultiChanAudioSource source = new AudioSource();
    private final Object lock = new Monitor("McPushFromMcPull");

    private int nbChans;
    private int maxInputLength;
    private int outputLength;
    private int queueCapacity;

    private int headOutToSkip;
    private int tailInToPad;

    private FloatQueue[] queues;
    private IMultiChanAudioSink sink;
    private Thread pusher;
    private long nbPushed; // Number of valid pushed samples
    private long nbPolled; // Number of polled samples (valid or not)


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanPullEffect) processingTarget, IMultiChanPushEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        nbChans = info.getNumberOfChannels();
        // Reset queue
        queues = null;
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
    public void process(float[][] samples, int length, IMultiChanAudioSink sink) {
        beginIfWorth();
        if (length > maxInputLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        assert super.isSane(samples, true);
        synchronized (lock) {
            if (queues == null) {
                queues = new FloatQueue[nbChans];
                for (int chan = 0; chan < nbChans; chan++)
                    queues[chan] = new FloatQueue(queueCapacity);
            }
            while (queues[0].getSize() + length > queueCapacity)
                waitLock();
            for (int chan = 0; chan < nbChans; chan++) {
                queues[chan].pushArray(samples[chan], length);
            }
            this.sink = sink;
            nbPushed += length;
            lock.notify();
        }
        if (length < samples[0].length)
            finishProcessing(sink);
    }

    private void finishProcessing(IMultiChanAudioSink sink) {
        float[][] zero = new float[nbChans][Math.min(maxInputLength, outputLength)];
        synchronized (lock) {
            while (pusher != null) {
                // Push zeroes until the last output block can be produced
                while (queues[0].getSize() + zero[0].length > queueCapacity) {
                    waitLock();
                    if (pusher == null)
                        return; // Finished
                }
                for (int chan = 0; chan < nbChans; chan++)
                    queues[chan].pushArray(zero[chan]);
                if (tailInToPad > 0) {
                    // Compensate for latency
                    int padAmount = Math.min(zero[0].length, tailInToPad);
                    nbPushed += padAmount; // Add tailInToPad zero samples as valid
                    tailInToPad -= padAmount;
                }
                lock.notify();
            }
        }
    }

    @Override
    public void stopProcessing() {
        finishProcessing(new IMultiChanAudioSink() {

            public void writeSamples(float[][] samples) {
                // Absorb
            }

            public void close() {
                // Ignore
            }
        });
        super.stopProcessing();
        queues = null;
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
        float[][] buffer = new float[nbChans][outputLength];
        // Processing loop
        while (true) {
            // Pull and process
            int count = processingTarget.process(source, buffer);
            // Write result to the sink
            if (count == buffer[0].length)
                writeSamples(sink, buffer);
            else if (count > 0) {
                float[][] temp = new float[nbChans][count];
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(buffer[chan], 0, temp[chan], 0, count);
                writeSamples(sink, temp);
            }
            if (count < buffer[0].length) {
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
    public void writeSamples(IMultiChanAudioSink sink, float[][] samples) {
        assert super.isSane(samples, false);
        int amount = samples[0].length;
        if (headOutToSkip >= amount)
            headOutToSkip -= amount;
        else if (headOutToSkip > 0) {
            float[][] head = new float[nbChans][amount - headOutToSkip];
            for (int chan = 0; chan < nbChans; chan++)
                System.arraycopy(samples[chan], headOutToSkip, head[chan], 0, head[chan].length);
            sink.writeSamples(head);
            headOutToSkip = 0;
        } else
            sink.writeSamples(samples);
    }

    class AudioSource implements IMultiChanAudioSource {

        public int readSamples(float[][] target) {
            synchronized (lock) {
                int offset = 0;
                int remaining = target[0].length;
                while (remaining > 0) {
                    int amount = Math.min(maxInputLength, remaining);
                    // Wait until enough data has been pushed
                    while (queues[0].getSize() < amount)
                        waitLock();
                    // Poll
                    for (int chan = 0; chan < nbChans; chan++)
                        queues[chan].popArray(target[chan], offset, amount);
                    nbPolled += amount;
                    lock.notify();
                    remaining -= amount;
                    offset += amount;
                }

                int result = target[0].length;
                if (nbPolled > nbPushed)
                    result = Math.max(0, result - (int) (nbPolled - nbPushed));
                McPushFromMcPull.super.processed(result);
                return result;
            }
        }

        public AudioStreamFormat getAudioFormat() {
            return ProcessingInfo.toAudioFormat(McPushFromMcPull.super.getHostInfo());
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
