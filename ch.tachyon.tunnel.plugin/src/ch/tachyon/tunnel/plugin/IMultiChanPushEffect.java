package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

public interface IMultiChanPushEffect extends IPlugin {

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

    public void process(float[][] input, int length, IMultiChanAudioSink target);

}
