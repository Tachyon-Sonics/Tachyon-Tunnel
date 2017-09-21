package ch.tachyon.tunnel.host.effect;

import ch.tachyon.tunnel.common.*;

public interface IPullEffect extends IEffect {

    public boolean canWriteFasterThanRead();

    /**
     * @return the number of sample frames produced. It must match the size of the output array,
     * unless the end of the stream has been reached.
     */
    public int process(IMultiChanAudioSource source, float[][] output);

}
