package ch.tachyon.tunnel.engine.bridges.chan.reduce;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.effect.*;

public class ScPushFromMcPush extends ScFromMcBase<ISingleChanPushEffect, IPushEffect> implements ISingleChanPushEffect {

    private IMultiChanAudioSink mcSink;
    
    
    @Override
    public Class<? extends IEffect> getInterface() {
        return ISingleChanPushEffect.class;
    }

    public boolean canWriteFasterThanRead() {
        return target.canWriteFasterThanRead();
    }

    public void process(float[] input, int length, ISingleChanAudioSink sink) {
        mcSamples[0] = input;
        if (mcSink == null)
            mcSink = new McSinkFromScSink(sink);
        target.process(mcSamples, length, mcSink);
    }
    
    @Override
    public void stopProcessing() {
        super.stopProcessing();
        mcSink = null;
    }

    static class McSinkFromScSink implements IMultiChanAudioSink {
        
        private final ISingleChanAudioSink target;

        
        public McSinkFromScSink(ISingleChanAudioSink target) {
            this.target = target;
        }
        public void writeSamples(float[][] samples) {
            assert samples.length == 1;
            target.writeSamples(samples[0]);
        }

        public void close() {
            target.close();
        }
        
    }

}
