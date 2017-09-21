package ch.tachyon.tunnel.engine.bridges;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Wraps a plugin implementing the {@link ISimpleEffect} interface into the host interface
 * {@link ch.tachyon.tunnel.host.effect.ISingleChanEffect}.
 */
public class ScEffectFromScEffect extends PluginToHostWrapper<ISimpleEffect> implements
        ch.tachyon.tunnel.host.effect.ISingleChanEffect {

    private Rebufferizer rebufferizer = null;


    @Override
    public void init(IPlugin baseTarget, IPlugin processingTarget, IMtContext mtContext, Properties properties) {
        super.init(baseTarget, (ISimpleEffect) processingTarget, ch.tachyon.tunnel.host.effect.ISingleChanEffect.class, mtContext, properties);
    }

    @Override
    public void startProcessing(ch.tachyon.tunnel.host.IProcessingInfo info) {
        super.startProcessing(info);
        if (super.needsRebuffering(false))
            rebufferizer = new Rebufferizer(getActualMaxChunkLength(info), getActualChunkLength());
        else
            rebufferizer = null;
    }

    public int getLatency() {
        if (rebufferizer == null)
            return super.getLatency(IoDirection.OUTPUT);
        else
            return super.getLatency(IoDirection.OUTPUT) + rebufferizer.getLatency();
    }

    public void process(float[] samples0) {
        assert super.isSane(samples0, true);
        beginIfWorth();
        float[] samples = wrapInput(samples0);
        if (rebufferizer == null)
            processingTarget.process(samples);
        else {
            rebufferizer.submitVariable(samples);
            float[] fixed = rebufferizer.retrieveFixed();
            while (fixed != null) {
                processingTarget.process(fixed);
                rebufferizer.submitFixed(fixed);
                fixed = rebufferizer.retrieveFixed();
            }
            rebufferizer.retrieveVariable(samples);
        }
        assert super.isSane(samples, false);
        super.unwrapInput(samples, samples0);
        super.processed(samples0.length);
    }

    @Override
    public void stopProcessing() {
        super.stopProcessing();
        rebufferizer = null;
    }

}
