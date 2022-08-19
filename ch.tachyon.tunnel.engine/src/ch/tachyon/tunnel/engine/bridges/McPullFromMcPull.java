package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;


public class McPullFromMcPull extends PluginToHostWrapper<IMultiChanPullEffect>
        implements ch.tachyon.tunnel.host.effect.IPullEffect, IMultiChanAudioSource {

    private IMultiChanPullEffect wrappedTarget;

    private IMultiChanAudioSource curSource;
    private int headOutToSkip;
    private int tailInToPad;
    private int maxOutputLength;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanPullEffect) processingTarget, ch.tachyon.tunnel.host.effect.IPullEffect.class, mtContext, properties);
    }

    private int getChunkLength(ch.tachyon.tunnel.host.IProcessingInfo info) {
        int result = getPreferredChunkLength();
        if (result > 0)
            return result;
        result = info.getHostPreferredChunkLength();
        if (result > 0)
            return result;
        return DEFAULT_CHUNK_LENGTH;
    }

    @Override
    public void startProcessing(final ch.tachyon.tunnel.host.IProcessingInfo info) {
        super.startProcessing(info);
        this.headOutToSkip = super.getLatency(IoDirection.OUTPUT);
        this.tailInToPad = super.getLatency(IoDirection.INPUT);
        this.maxOutputLength = getActualMaxChunkLength(info);
        if (super.needsRebuffering() || headOutToSkip > 0) {
            /*
             * Wrap target into a version that uses a fixed block size
             */
            this.wrappedTarget = new IMultiChanPullEffect() {

                private float[][] buffer;
                private int bufferIndex;
                private int bufferAvailable;

                public int process(IMultiChanAudioSource source, float[][] output) {
                    if (buffer == null) {
                        buffer = new float[output.length][getChunkLength(info)];
                        bufferIndex = buffer[0].length;
                    }
                    int outIndex = 0;
                    int result = 0;
                    while (outIndex < output[0].length) {
                        if (bufferIndex >= buffer[0].length) {
                            bufferAvailable = processingTarget.process(source, buffer);
                            bufferIndex = 0;
                        }
                        int amount = Math.min(output[0].length - outIndex, buffer[0].length - bufferIndex);
                        for (int chan = 0; chan < output.length; chan++)
                            System.arraycopy(buffer[chan], bufferIndex, output[chan], outIndex, amount);
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

    public int process(IMultiChanAudioSource source, float[][] output) {
        beginIfWorth();
        if (output[0].length > maxOutputLength)
            throw new IllegalArgumentException("Host error: the given array length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        this.curSource = source;
        if (headOutToSkip > 0) {
            // Drop first samples
            float[][] temp = new float[output.length][headOutToSkip];
            wrappedTarget.process(this, temp);
            headOutToSkip = 0;
        }
        int result = wrappedTarget.process(this, output);
        assert isSane(output, false);
        return result;
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
            for (int chan = 0; chan < target.length; chan++)
                Arrays.fill(target[chan], result, result + amount, 0.0f);
            tailInToPad -= amount;
            result += amount;
        }
        assert isSane(target, true);
        super.processed(result);
        return result;
    }

    public AudioStreamFormat getAudioFormat() {
        return curSource.getAudioFormat();
    }

    public void close() {
        curSource.close();
    }

}
