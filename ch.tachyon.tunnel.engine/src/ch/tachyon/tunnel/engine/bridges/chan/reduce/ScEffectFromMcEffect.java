package ch.tachyon.tunnel.engine.bridges.chan.reduce;

import ch.tachyon.tunnel.host.effect.*;

public class ScEffectFromMcEffect extends ScFromMcBase<ISingleChanEffect, ISimpleEffect> implements ISingleChanEffect {

    @Override
    public Class<? extends IEffect> getInterface() {
        return ISingleChanEffect.class;
    }

    public int getLatency() {
        return target.getLatency();
    }

    public void process(float[] samples) {
        mcSamples[0] = samples;
        target.process(mcSamples);
    }

}
