package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.ISimpleEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link ISimpleEffect} interface into the host {@link ISingleChanPushEffect}
 * interface.
 */
public class ScPushFromScEffect extends PluginToHostWrapper<ISimpleEffect> implements ISingleChanPushEffect,
        ISingleChanAudioSink {

    private Rebufferizer rebufferizer = null;
    private ISingleChanAudioSink curSink;
    private int maxChunkSize;
    private int headOutToSkip;
    private int tailInToPad;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (ISimpleEffect) processingTarget, ISingleChanPushEffect.class, mtContext, properties);
    }

    public boolean canWriteFasterThanRead() {
        return false;
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        maxChunkSize = getActualMaxChunkLength(info);
        if (needsRebuffering()) {
            int chunkSize = getActualChunkLength();
            rebufferizer = new Rebufferizer(maxChunkSize, chunkSize);
        } else
            rebufferizer = null;
        this.headOutToSkip = getFullLatency(IoDirection.OUTPUT);
        this.tailInToPad = getFullLatency(IoDirection.INPUT);
    }

    @Override
    protected boolean needsRebuffering() {
        if (super.needsRebuffering())
            return true;
        return super.getLatency(IoDirection.INPUT) > 0 || super.getLatency(IoDirection.OUTPUT) > 0;
    }

    private int getFullLatency(IoDirection ioDirection) {
        if (rebufferizer == null)
            return super.getLatency(ioDirection);
        else
            return super.getLatency(ioDirection) + rebufferizer.getLatency();
    }

    public void process(float[] input, int length, ISingleChanAudioSink sink) {
        beginIfWorth();
        assert isSane(input, true);
        if (length > maxChunkSize)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        input = super.wrapInput(input);
        this.curSink = sink;
        if (input.length == length)
            process0(input, length);
        else {
            // Last block - pad for latency
            int amount = Math.min(maxChunkSize, length + tailInToPad);
            float[] temp = new float[amount];
            System.arraycopy(input, 0, temp, 0, Math.min(length, amount));
            process0(temp, amount);
            tailInToPad -= (amount - length);
            while (tailInToPad > 0) {
                amount = Math.min(maxChunkSize, tailInToPad);
                temp = new float[amount];
                process0(temp, amount);
                tailInToPad -= amount;
            }
        }
    }

    private void process0(float[] input, int length) {
        if (rebufferizer != null) {
            float[] data;
            if (input.length == length)
                data = input;
            else {
                data = new float[length];
                System.arraycopy(input, 0, data, 0, length);
            }
            rebufferizer.submitVariable(data);
            float[] fixed = rebufferizer.retrieveFixed();
            while (fixed != null) {
                processingTarget.process(fixed);
                rebufferizer.submitFixed(fixed);
                fixed = rebufferizer.retrieveFixed();
            }
            rebufferizer.retrieveVariable(data);
            writeSamples(data);
        } else {
            if (length < input.length)
                Arrays.fill(input, length, input.length, 0.0f); // Zero pad input
            processingTarget.process(input);
            if (length == input.length)
                writeSamples(input);
            else {
                // Truncate output
                float[] temp = new float[length];
                System.arraycopy(input, 0, temp, 0, length);
                writeSamples(temp);
            }
        }
        super.processed(length);
    }

    // IAudioSink

    public void writeSamples(float[] samples) {
        assert super.isSane(samples, false);
        if (headOutToSkip >= samples.length)
            headOutToSkip -= samples.length;
        else if (headOutToSkip > 0) {
            float[] temp = new float[samples.length - headOutToSkip];
            System.arraycopy(samples, headOutToSkip, temp, 0, temp.length);
            curSink.writeSamples(temp);
            headOutToSkip = 0;
        } else
            curSink.writeSamples(samples);
    }

    public void close() {
        curSink.close();
    }

}
