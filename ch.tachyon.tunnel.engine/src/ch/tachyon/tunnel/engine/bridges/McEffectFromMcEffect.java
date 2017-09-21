package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link IMultiChanEffect} interface into the host interface
 * {@link ch.tachyon.tunnel.host.effect.ISimpleEffect}.
 */
public class McEffectFromMcEffect extends PluginToHostWrapper<IMultiChanEffect>
        implements ch.tachyon.tunnel.host.effect.ISimpleEffect {

    private McRebufferizer rebufferizer = null;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (IMultiChanEffect) processingTarget, ch.tachyon.tunnel.host.effect.ISimpleEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(ch.tachyon.tunnel.host.IProcessingInfo info) {
        super.startProcessing(info);
        if (super.needsRebuffering(false))
            rebufferizer = new McRebufferizer(info.getNumberOfChannels(), getActualMaxChunkLength(info), getActualChunkLength());
        else
            rebufferizer = null;
    }

    public int getLatency() {
        if (rebufferizer == null)
            return super.getLatency(IoDirection.OUTPUT);
        else
            return super.getLatency(IoDirection.OUTPUT) + rebufferizer.getLatency();
    }

    public void process(float[][] samples) {
        beginIfWorth();
        if (rebufferizer == null)
            processingTarget.process(samples);
        else {
            rebufferizer.submitVariable(samples);
            float[][] fixed = rebufferizer.retrieveFixed();
            while (fixed != null) {
                processingTarget.process(fixed);
                rebufferizer.submitFixed(fixed);
                fixed = rebufferizer.retrieveFixed();
            }
            rebufferizer.retrieveVariable(samples);
        }
        super.processed(samples[0].length);
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        rebufferizer = null;
    }

}
