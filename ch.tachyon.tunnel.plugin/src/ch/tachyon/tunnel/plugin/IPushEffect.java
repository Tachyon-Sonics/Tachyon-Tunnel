package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;

public interface IPushEffect extends IPlugin {

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
     * Process the given input samples. The input array should not be modified, instead the processed
     * samples must be writen using {@link ISingleChanAudioSink#writeSamples(float[])} using the
     * 'target' argument.
     * <p>
     * The {@link ISingleChanAudioSink#writeSamples(float[])} can be invoked any number of times
     * with arrays of any size.
     * <p>
     * Unlike {@link IPullEffect}, there are no restriction when using {@link MultiThreading}.
     * @param input the input audio chunk to process
     * @param length the length of the chunk. It is guaranteed that this value is smaller than
     * <code>input.length</code> exactly once, namely for the last block. Note that a length of
     * zero might be used for the last block if the processed audio exactly stops at the full length
     * of the previous block. If the length is smaller than <code>input.length</code> the implementation
     * must ensure to flush all the remaining cached samples (if any) to the {@link ISingleChanAudioSink}
     * @param target a callback with which the produced audio can be send
     */
    public void process(float[] input, int length, ISingleChanAudioSink target);

}
