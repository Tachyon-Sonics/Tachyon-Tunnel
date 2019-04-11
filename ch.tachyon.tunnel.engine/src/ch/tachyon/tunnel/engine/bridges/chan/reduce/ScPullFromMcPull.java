package ch.tachyon.tunnel.engine.bridges.chan.reduce;

import javax.sound.sampled.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.effect.*;

public class ScPullFromMcPull extends ScFromMcBase<ISingleChanPullEffect, IPullEffect> implements ISingleChanPullEffect {

    private IMultiChanAudioSource mcSource;
    
    
    @Override
    public Class<? extends IEffect> getInterface() {
        return ISingleChanPullEffect.class;
    }

    public boolean canWriteFasterThanRead() {
        return target.canWriteFasterThanRead();
    }

    public int process(ISingleChanAudioSource source, float[] output) {
        mcSamples[0] = output;
        if (mcSource == null)
            mcSource = new McSourceFromScSource(source);
        return target.process(mcSource, mcSamples);
    }
    
    @Override
    public void stopProcessing() {
        super.stopProcessing();
        mcSource = null;
    }

    static class McSourceFromScSource implements IMultiChanAudioSource {
        
        private final ISingleChanAudioSource target;

        
        public McSourceFromScSource(ISingleChanAudioSource target) {
            this.target = target;
        }

        public AudioFormat getAudioFormat() {
            return target.getAudioFormat();
        }

        public int readSamples(float[][] output) {
            assert output.length == 1;
            return target.readSamples(output[0]);
        }

        public void close() {
            target.close();
        }
        
    }

}
