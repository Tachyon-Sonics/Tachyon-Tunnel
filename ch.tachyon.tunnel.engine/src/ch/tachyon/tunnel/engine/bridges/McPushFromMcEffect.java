package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.IPushEffect;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link IMultiChanEffect} interface into the host {@link IPushEffect}
 * interface.
 */
public class McPushFromMcEffect extends PluginToHostWrapper<IMultiChanEffect> implements IPushEffect,
        IMultiChanAudioSink {

    private McRebufferizer rebufferizer = null;
    private IMultiChanAudioSink curSink;
    private int nbChans;
    private int maxChunkSize;
    private int headOutToSkip;
    private int tailInToPad;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanEffect) processingTarget, IMultiChanPushEffect.class, mtContext, properties);
    }

    public boolean canWriteFasterThanRead() {
        return false;
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        super.startProcessing(info);
        nbChans = info.getNumberOfChannels();
        maxChunkSize = getActualMaxChunkLength(info);
        if (needsRebuffering()) {
            int chunkSize = getActualChunkLength();
            rebufferizer = new McRebufferizer(nbChans, maxChunkSize, chunkSize);
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

    public void process(float[][] input, int length, IMultiChanAudioSink sink) {
        beginIfWorth();
        assert input.length == nbChans : "Illegal number of channels";
        assert isSane(input, true);
        if (length > maxChunkSize)
            throw new IllegalArgumentException("Host error: the given length is greater than the maximum length specified " +
                        "by the IProcessingInfo object passed to startProcessing()");
        input = super.wrapInput(input);
        this.curSink = sink;
        if (input[0].length == length)
            process0(input, length);
        else {
            // Last block - pad for latency
            int amount = Math.min(maxChunkSize, length + tailInToPad);
            float[][] temp = new float[nbChans][amount];
            for (int chan = 0; chan < nbChans; chan++)
                System.arraycopy(input[chan], 0, temp[chan], 0, Math.min(length, amount));
            process0(temp, amount);
            tailInToPad -= (amount - length);
            while (tailInToPad > 0) {
                amount = Math.min(maxChunkSize, tailInToPad);
                temp = new float[nbChans][amount];
                process0(temp, amount);
                tailInToPad -= amount;
            }
        }
    }

    private void process0(float[][] input, int length) {
        if (rebufferizer != null) {
            float[][] data;
            if (input[0].length == length)
                data = input;
            else {
                data = new float[nbChans][length];
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(input[chan], 0, data[chan], 0, length);
            }
            rebufferizer.submitVariable(data);
            float[][] fixed = rebufferizer.retrieveFixed();
            while (fixed != null) {
                processingTarget.process(fixed);
                rebufferizer.submitFixed(fixed);
                fixed = rebufferizer.retrieveFixed();
            }
            rebufferizer.retrieveVariable(data);
            writeSamples(data);
        } else {
            if (length < input[0].length) {
                for (int chan = 0; chan < nbChans; chan++)
                    Arrays.fill(input[chan], length, input[chan].length, 0.0f); // Zero pad input
            }
            processingTarget.process(input);
            if (length == input[0].length)
                writeSamples(input);
            else {
                // Truncate output
                float[][] temp = new float[nbChans][length];
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(input[chan], 0, temp[chan], 0, length);
                writeSamples(temp);
            }
        }
        super.processed(length);
    }

    // IAudioSink

    public void writeSamples(float[][] samples) {
        assert super.isSane(samples, false);
        if (headOutToSkip >= samples[0].length)
            headOutToSkip -= samples[0].length;
        else if (headOutToSkip > 0) {
            float[][] temp = new float[nbChans][samples[0].length - headOutToSkip];
            for (int chan = 0; chan < nbChans; chan++)
                System.arraycopy(samples[chan], headOutToSkip, temp[chan], 0, temp[chan].length);
            curSink.writeSamples(temp);
            headOutToSkip = 0;
        } else
            curSink.writeSamples(samples);
    }

    public void close() {
        curSink.close();
    }

}
