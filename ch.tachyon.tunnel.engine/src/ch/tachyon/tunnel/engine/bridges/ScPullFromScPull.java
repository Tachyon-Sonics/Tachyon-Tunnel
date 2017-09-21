package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.IPullEffect;
import ch.tachyon.tunnel.plugin.opt.thread.*;


public class ScPullFromScPull extends PluginToHostWrapper<IPullEffect> implements ch.tachyon.tunnel.host.effect.ISingleChanPullEffect, ISingleChanAudioSource {

    private IPullEffect wrappedTarget;

    private ISingleChanAudioSource curSource;
    private int headOutToSkip;
    private int tailInToPad;
    private int maxOutputLength;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IPullEffect) processingTarget, ISingleChanPullEffect.class, mtContext, properties);
    }

    private int getChunkLength(IProcessingInfo info) {
        int result = getPreferredChunkLength();
        if (result > 0)
            return result;
        result = info.getHostPreferredChunkLength();
        if (result > 0)
            return result;
        return DEFAULT_CHUNK_LENGTH;
    }

    @Override
    public void startProcessing(final IProcessingInfo info) {
        super.startProcessing(info);
        this.headOutToSkip = super.getLatency(IoDirection.OUTPUT);
        this.tailInToPad = super.getLatency(IoDirection.INPUT);
        this.maxOutputLength = getActualMaxChunkLength(info);
        if (super.needsRebuffering() || headOutToSkip > 0) {
            /*
             * Wrap target into a version that uses a fixed block size
             */
            this.wrappedTarget = new IPullEffect() {

                private final float[] buffer = new float[getChunkLength(info)];
                private int bufferIndex = buffer.length;
                private int bufferAvailable;
                
                public int process(ISingleChanAudioSource source, float[] output) {
                    int outIndex = 0;
                    int result = 0;
                    while (outIndex < output.length) {
                        if (bufferIndex >= buffer.length) {
                            bufferAvailable = processingTarget.process(source, buffer);
                            bufferIndex = 0;
                        }
                        int amount = Math.min(output.length - outIndex, buffer.length - bufferIndex);
                        System.arraycopy(buffer, bufferIndex, output, outIndex, amount);
                        result += Math.max(0, Math.min(amount, bufferAvailable - bufferIndex));
                        outIndex += amount;
                        bufferIndex += amount;
                    }
                    return result;
                }
                
                public boolean canWriteFasterThanRead() {
                    return processingTarget.canWriteFasterThanRead();
                }
            };
        } else
            this.wrappedTarget = processingTarget;
    }

    public boolean canWriteFasterThanRead() {
        return processingTarget.canWriteFasterThanRead();
    }

    public int process(ISingleChanAudioSource source, float[] output) {
        beginIfWorth();
        if (output.length > maxOutputLength)
            throw new IllegalArgumentException("Host error: the given array length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        this.curSource = source;
        if (headOutToSkip > 0) {
            // Drop first samples to compensate latency, if any
            float[] temp = new float[headOutToSkip];
            wrappedTarget.process(this, temp);
            headOutToSkip = 0;
        }
        int result = wrappedTarget.process(this, output);
        assert isSane(output, false);
        return result;
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        this.curSource = null;
        this.wrappedTarget = null;
    }

    // IAudioSource

    /**
     * Compensate latency by padding the tail of the original source with zeros
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
        super.processed(result);
        return result;
    }

    public AudioFormat getAudioFormat() {
        return ProcessingInfo.toAudioFormat(super.getHostInfo());
    }

    public void close() {
        curSource.close();
    }

}
