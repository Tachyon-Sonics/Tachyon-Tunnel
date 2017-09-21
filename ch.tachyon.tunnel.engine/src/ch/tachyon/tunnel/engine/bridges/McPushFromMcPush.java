package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.IPushEffect;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class McPushFromMcPush extends PluginToHostWrapper<IMultiChanPushEffect> implements IPushEffect, IMultiChanAudioSink {

    private IMultiChanAudioSink curSink;
    private McRebufferizer rebufferizer = null;
    private int queued; // Samples written in McRebufferizer that are not yet read
    private int headOutToSkip;
    private int tailInToPad;
    private int maxInputLength;
    private boolean lastChunkReceived;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanPushEffect) processingTarget, IPushEffect.class, mtContext, properties);
    }

    public boolean canWriteFasterThanRead() {
        return processingTarget.canWriteFasterThanRead();
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        this.headOutToSkip = super.getLatency(IoDirection.OUTPUT);
        this.tailInToPad = super.getLatency(IoDirection.INPUT);
        info.negociateFixedChunkLength(getActualChunkLength());
        this.maxInputLength = getActualMaxChunkLength(info);

        if (super.needsRebuffering())
            rebufferizer = new McRebufferizer(info.getNumberOfChannels(), maxInputLength, getActualChunkLength());
        else
            rebufferizer = null;
        queued = 0;
        lastChunkReceived = false;
    }

    public void process(float[][] input, int length, IMultiChanAudioSink sink) {
        beginIfWorth();
        int nbChans = input.length;
        int nbSamples = input[0].length;
        for (int c = 0; c < nbChans; c++)
            assert super.isSane(input[c], true);
        if (length > maxInputLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        if (lastChunkReceived)
            throw new IllegalArgumentException("process() was already invoked with a length less than the array length, meaning that it was the last block. No further call to process() shall be done."
                    + " Perhaps you mean to invoke stopProcessing() and startProcessing() to start a new audio chunk");
        this.curSink = sink;
        if (length == nbSamples) {
            if (rebufferizer == null) {
                input = super.wrapInput(input);
                processingTarget.process(input, length, this);
            } else {
                rebufferizer.submitVariable(input);
                queued += length;
                float[][] fixed = rebufferizer.retrieveFixed();
                while (fixed != null) {
                    queued -= fixed[0].length;
                    fixed = super.wrapInput(fixed);
                    processingTarget.process(fixed, rebufferizer.getTargetSize(), this);
                    fixed = rebufferizer.retrieveFixed();
                }
            }
        } else {
            // This is the last audio chunk
            lastChunkReceived = true;
            int remaining = length + tailInToPad + queued;
            boolean lastSent = false;
            while (remaining > 0) {
                for (int c = 0; c < nbChans; c++)
                    Arrays.fill(input[c], length, nbSamples, 0.0f); // Pad with zero
                if (rebufferizer == null) {
                    input = super.wrapInput(input);
                    int amount = Math.min(remaining, nbSamples);
                    if (amount < nbSamples)
                        lastSent = true;
                    processingTarget.process(input, amount, this);
                    length = Math.max(0, length - amount);
                    remaining -= amount;
                } else {
                    rebufferizer.submitVariable(input);
                    int fixedLength = rebufferizer.getTargetSize();
                    float[][] fixed = rebufferizer.retrieveFixed();
                    while (fixed != null && remaining > 0) {
                        fixed = super.wrapInput(fixed);
                        int amount = Math.min(remaining, fixedLength);
                        if (amount < fixedLength)
                            lastSent = true;
                        processingTarget.process(fixed, amount, this);
                        fixed = rebufferizer.retrieveFixed();
                        length = Math.max(0, length - amount);
                        remaining -= amount;
                    }
                }
            }
            if (!lastSent) {
                /*
                 * If lastSent == false, we have never invoked process() with a length less than the
                 * array length, and the plugin hence does not know this is the last block.
                 * Therefore we must invoke process an additional time (with length == 0) to notify
                 * the plugin that this is the last block.
                 */
                if (rebufferizer != null) {
                    float[][] last = new float[nbChans][rebufferizer.getTargetSize()];
                    processingTarget.process(last, 0, this);
                } else {
                    float[][] last = new float[nbChans][nbSamples];
                    processingTarget.process(last, 0, this);
                }
            }
            tailInToPad = 0;
        }
        super.processed(length);
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        this.curSink = null;
    }

    // IAudioSink

    public void writeSamples(float[][] samples) {
        int nbChans = samples.length;
        int amount = samples[0].length;
        for (int c = 0; c < nbChans; c++)
            assert super.isSane(samples[c], false);
        if (headOutToSkip >= amount) {
            headOutToSkip -= amount;
        } else if (headOutToSkip > 0) {
            float[][] head = new float[nbChans][amount - headOutToSkip];
            for (int c = 0; c < nbChans; c++)
                System.arraycopy(samples[c], headOutToSkip, head[c], 0, head[c].length);
            curSink.writeSamples(head);
            headOutToSkip = 0;
        } else
            curSink.writeSamples(samples);
    }

    public void close() {
        curSink.close();
    }

}
