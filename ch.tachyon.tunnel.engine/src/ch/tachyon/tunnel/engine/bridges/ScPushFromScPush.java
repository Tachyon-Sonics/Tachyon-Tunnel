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

/**
 * Wraps a plugin implementing the {@link IPushEffect} interface into the host {@link ISingleChanPushEffect}
 * interface.
 */
public class ScPushFromScPush extends PluginToHostWrapper<IPushEffect> implements ISingleChanPushEffect, ISingleChanAudioSink {

    private ISingleChanAudioSink curSink;
    private Rebufferizer rebufferizer = null;
    private int queued; // Samples written in Rebufferizer that are not yet read
    private int headOutToSkip;
    private int tailInToPad;
    private int maxInputLength;
    private boolean lastChunkRecieved;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IPushEffect) processingTarget, ISingleChanPushEffect.class, mtContext, properties);
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
            rebufferizer = new Rebufferizer(maxInputLength, getActualChunkLength());
        else
            rebufferizer = null;
        queued = 0;
        lastChunkRecieved = false;
    }

    public void process(float[] input, int length, ISingleChanAudioSink sink) {
        beginIfWorth();
        assert super.isSane(input, true);
        if (length > maxInputLength)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        if (lastChunkRecieved)
            throw new IllegalArgumentException("process() was already invoked with a length less than the array length, meaning that it was the last block. No further call to process() shall be done."
                    + " Perhaps you mean to invoke stopProcessing() and startProcessing() to start a new audio chunk");
        this.curSink = sink;
        if (length == input.length) {
            if (rebufferizer == null) {
                input = super.wrapInput(input);
                processingTarget.process(input, length, this);
            } else {
                rebufferizer.submitVariable(input);
                queued += length;
                float[] fixed = rebufferizer.retrieveFixed();
                while (fixed != null) {
                    queued -= fixed.length;
                    fixed = super.wrapInput(fixed);
                    processingTarget.process(fixed, fixed.length, this);
                    fixed = rebufferizer.retrieveFixed();
                }
            }
        } else {
            // This is the last audio chunk
            lastChunkRecieved = true;
            int remaining = length + tailInToPad + queued;
            boolean lastSent = false;
            while (remaining > 0) {
                Arrays.fill(input, length, input.length, 0.0f); // Pad with zero
                if (rebufferizer == null) {
                    input = super.wrapInput(input);
                    int amount = Math.min(remaining, input.length);
                    if (amount < input.length)
                        lastSent = true;
                    processingTarget.process(input, amount, this);
                    length = Math.max(0, length - amount);
                    remaining -= amount;
                } else {
                    rebufferizer.submitVariable(input);
                    float[] fixed = rebufferizer.retrieveFixed();
                    while (fixed != null && remaining > 0) {
                        fixed = super.wrapInput(fixed);
                        int amount = Math.min(remaining, fixed.length);
                        if (amount < fixed.length)
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
                    float[] last = new float[rebufferizer.getTargetSize()];
                    processingTarget.process(last, 0, this);
                } else {
                    float[] last = new float[input.length];
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

    public void writeSamples(float[] samples) {
        assert super.isSane(samples, false);
        int amount = samples.length;
        if (headOutToSkip >= amount) {
            headOutToSkip -= amount;
        } else if (headOutToSkip > 0) {
            float[] head = new float[amount - headOutToSkip];
            System.arraycopy(samples, headOutToSkip, head, 0, head.length);
            curSink.writeSamples(head);
            headOutToSkip = 0;
        } else
            curSink.writeSamples(samples);
    }

    public void close() {
    }

}
