package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.effect.IPullEffect;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link IMultiChanEffect} interface into the host interface
 * {@link IPullEffect}.
 */
public class McPullFromMcEffect extends PluginToHostWrapper<IMultiChanEffect> implements IPullEffect, IMultiChanAudioSource {

    private McRebufferizer rebufferizer;
    private IMultiChanAudioSource curSource;
    private int maxChunkSize;
    private int headOutToSkip;
    private int tailInToPad;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanEffect) processingTarget, IPullEffect.class, mtContext, properties);
    }

    public boolean canWriteFasterThanRead() {
        return false;
    }

    @Override
    public void startProcessing(ch.tachyon.tunnel.host.IProcessingInfo info) {
        super.startProcessing(info);
        if (needsRebuffering()) {
            maxChunkSize = getActualMaxChunkLength(info);
            rebufferizer = new McRebufferizer(info.getNumberOfChannels(), maxChunkSize, getActualChunkLength());
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

    public int process(IMultiChanAudioSource source, float[][] output) {
        beginIfWorth();
        this.curSource = source;
        while (headOutToSkip > 0) {
            // Drop first samples to compensate latency, if any
            int amount = Math.min(headOutToSkip, maxChunkSize);
            float[][] temp = new float[output.length][amount];
            this.readSamples(temp);
            process0(temp);
            headOutToSkip -= amount;
        }
        int count = this.readSamples(output);
        process0(output);
        super.processed(count);
        return count;
    }

    // McFixedSizeProcessor

    protected void process0(float[][] output) {
        if (rebufferizer == null)
            processingTarget.process(output);
        else {
            rebufferizer.submitVariable(output);
            float[][] fixed = rebufferizer.retrieveFixed();
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
    public int readSamples(float[][] target) {
        int result = curSource.readSamples(target);
        if (result < target[0].length && tailInToPad > 0) {
            // Pad with zero to compensate latency, if any
            int amount = Math.min(target[0].length - result, tailInToPad);
            for (int i = 0; i < target.length; i++)
                Arrays.fill(target[i], result, result + amount, 0.0f);
            tailInToPad -= amount;
            result += amount;
        }
        return result;
    }

    public AudioStreamFormat getAudioFormat() {
        return curSource.getAudioFormat();
    }

    public void close() {
        curSource.close();
    }

}
