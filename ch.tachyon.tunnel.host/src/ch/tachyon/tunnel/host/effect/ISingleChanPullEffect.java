package ch.tachyon.tunnel.host.effect;

import ch.tachyon.tunnel.common.*;

public interface ISingleChanPullEffect extends IEffect {

    public boolean canWriteFasterThanRead();

    /**
     * @param source the source from which input samples can be read. The object will be the same
     * during the whole processing.
     * @return the number of sample frames produced. It must match the size of the output array,
     * unless the source failed to read all the required input samples.
     */
    public int process(ISingleChanAudioSource source, float[] output);

}
