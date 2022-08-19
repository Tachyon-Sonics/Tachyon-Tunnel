package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.ISimpleEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;


public class ScPullFromScEffect extends PluginToHostWrapper<ISimpleEffect> implements ISingleChanPullEffect, ISingleChanAudioSource {

    private Rebufferizer rebufferizer = null;
    private ISingleChanAudioSource curSource;
    private int maxChunkSize;
    private int headOutToSkip;
    private int tailInToPad;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (ISimpleEffect) processingTarget, ISingleChanPullEffect.class, mtContext, properties);
    }

    public boolean canWriteFasterThanRead() {
        return false;
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        maxChunkSize = getActualMaxChunkLength(info);
        if (needsRebuffering()) {
            rebufferizer = new Rebufferizer(maxChunkSize, getActualChunkLength());
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

    public int process(ISingleChanAudioSource source, float[] output) {
        beginIfWorth();
        if (output.length > maxChunkSize)
            throw new IllegalArgumentException("Host error: the given array length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        this.curSource = source;
        while (headOutToSkip > 0) {
            // Drop first samples to compensate latency, if any
            int amount = Math.min(headOutToSkip, maxChunkSize);
            float[] temp = new float[amount];
            this.readSamples(temp);
            process0(temp);
            headOutToSkip -= amount;
        }
        int count = this.readSamples(output);
        process0(output);
        assert isSane(output, false);
        super.processed(count);
        return count;
    }

    protected void process0(float[] output) {
        if (rebufferizer == null)
            processingTarget.process(output);
        else {
            rebufferizer.submitVariable(output);
            float[] fixed = rebufferizer.retrieveFixed();
            while (fixed != null) {
                processingTarget.process(fixed);
                rebufferizer.submitFixed(fixed);
                fixed = rebufferizer.retrieveFixed();
            }
            rebufferizer.retrieveVariable(output);
        }
    }

    // IAudioSource

    /**
     * Compensate latency by padding the original source with zeros
     */
    public int readSamples(float[] target) {
        int result = curSource.readSamples(target);
        if (result < target.length && tailInToPad > 0) {
            // Pad with zero to compensate latency, if any
            int amount = Math.min(target.length - result, tailInToPad);
            Arrays.fill(target, result, result + amount, 0.0f);
            tailInToPad -= amount;
            result += amount;
        }
        assert isSane(target, true);
        return result;
    }

    public AudioStreamFormat getAudioFormat() {
        return ProcessingInfo.toAudioFormat(super.getHostInfo());
    }

    public void close() {
        curSource.close();
    }

}
