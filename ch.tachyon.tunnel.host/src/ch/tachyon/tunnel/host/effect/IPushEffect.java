package ch.tachyon.tunnel.host.effect;

import ch.tachyon.tunnel.common.*;

public interface IPushEffect extends IEffect {

    public boolean canWriteFasterThanRead();

    public void process(float[][] input, int length, IMultiChanAudioSink target);

}
