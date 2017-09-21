package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Interface for plugins implemented using a PULL approach. In a PULL approach, the plugin
 * has to produce output by chunks. For each chunk of audio output the plugin can read as much input
 * audio data as needed using an {@link IMultiChanAudioSource} object.
 * <p>
 * This interface can be used for plugins that change the speed of the audio (such as resampling
 * or time-stretching), or by any plugin that has to read audio input at irregular rates.
 * <p>
 * A plugin implementing this interface is responsible for producing a zero-latency output. Unlike with
 * {@link IMultiChanEffect} and {@link ISimpleEffect}, the host will not do latency compensation.
 */
public interface IMultiChanPullEffect extends IPlugin {

    /**
     * Whether the plugin might, at any time during the processing, be in a situation where more
     * audio data has been produced than the amount of audio data that has been read.
     * <p>
     * A value of <code>true</code> informs the host that it must create a backup to perform read
     * operations to prevent any read operation to read samples that have been overwritten.
     * <p>
     * Typically, a resampling plugin (or any plugin that changes the audio speed), must return
     * <code>true</code> when speeding up the audio, and can return <code>false</code> when slowing down.
     * Plugins that do not change the audio speed can always return <code>false</code>.
     * <p>
     * This method is invoked after the parameters have been set, before the processing starts, just
     * after {@link IStartStop#startProcessing(IProcessingInfo)} if implemented. Hence the returned
     * value may depend on the values of the parameter. The result is not allowed to change while processing;
     * hence if a sweepable parameter might change the result the plugin should always return <code>true</code>
     * for safety.
     * <p>
     * A value of <code>true</code> basically informs the host that it can not read and write using
     * the same buffer, and must instead use two different buffers for reading and writing, to ensure
     * that the written samples do not overwrite samples that have not been read yet. Note that using two
     * different buffers might involve additional resources from the host.
     * @return whether the plugin may write more samples than it has read
     */
    public boolean canWriteFasterThanRead();

    /**
     * Process as much audio as required in order to fill the given output buffer.
     * @param source the source from which input samples can be read.
     * @param output the output buffer to fill. The first dimension is the channel number and the second
     * dimension is the sample number.
     * @return the number of sample frames that were actually processed. It must match the given
     * output buffer size, unless {@link IMultiChanAudioSource#readSamples(float[][])} did not read all the
     * requested samples.
     */
    public int process(IMultiChanAudioSource source, float[][] output);

}
