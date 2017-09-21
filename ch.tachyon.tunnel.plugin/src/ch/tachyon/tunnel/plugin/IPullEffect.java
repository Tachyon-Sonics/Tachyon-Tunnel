package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;


public interface IPullEffect extends IPlugin {

    /**
     * Whether the plugin can write more samples than it reads, i.e., whether the plugin
     * might transform the audio into a longer version.
     * <p>
     * When <code>true</code>, this informs the host that it cannot do the processing
     * in-place because samples might be overwritten before they are read.
     * <p>
     * This method is invoked once per audio section to process, before the processing starts.
     * <p>
     * If the result depends on the value of a parameter:
     * <ul>
     * <li>If the parameter is {@link Sweepable}, <code>true</code> must be returned, because
     * the value of the parameter can be changed at any time during the processing.
     * <li>Else, the result should match the current value of the parameter.
     * </ul>
     */
    public boolean canWriteFasterThanRead();

    /**
     * Try to fill the given output array by reading as much input samples as required from the given source.
     * <p>
     * The implementation must ensure that
     * <ul>
     * <li>If the required number of samples can be read from the source, the entire output array
     * must be filled and its size must be returned.
     * <li>If the required number of samples cannot be read from the source, a number of samples smaller than
     * the output array size must be produced and returned, either by the current invocation or by a
     * subsequent one. Failure to do so may result in an infinite loop. Indeed, a return result smaller than
     * the given array is the only indication to the host that all input samples that could be read from the
     * source have been processed into output samples.
     * </ul>
     * It is legal to return <code>0</code> if no sample at all can be read from the source.
     * <p>
     * If the plugin supports multi-threading as specified by the {@link MultiThreading} annotation, the
     * following must also hold to allow efficient parallelization on multi-core hardware:
     * <ul>
     * <li>The method must call {@link ISingleChanAudioSource#readSamples(float[])} <i>exactly once</i>
     * per invocation. It is legal to request any number of samples, including 0 though. There is no need
     * to syncronize the call.
     * <li>The method must expect to be invoked several times <i>after</i> the last audio chunk has been processed.
     * On all these invocations, {@link ISingleChanAudioSource#readSamples(float[])} will return 0, and
     * the method must appropriately return 0 as well.
     * </ul>
     * @param source the source from which to read the samples from
     * @param output the output array to fill with the result of the processing
     * @return the actual number of output samples produced
     */
    public int process(ISingleChanAudioSource source, float[] output);

}
