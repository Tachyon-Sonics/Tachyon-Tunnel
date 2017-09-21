package ch.tachyon.tunnel.host.effect;

import ch.tachyon.tunnel.common.*;

public interface ISingleChanPushEffect extends IEffect {

    public boolean canWriteFasterThanRead();

    public void process(float[] input, int length, ISingleChanAudioSink target);

}
