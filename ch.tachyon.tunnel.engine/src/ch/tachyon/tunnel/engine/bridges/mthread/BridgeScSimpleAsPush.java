package ch.tachyon.tunnel.engine.bridges.mthread;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;


public class BridgeScSimpleAsPush extends BridgeScSimpleBase implements IPushEffect, IDuplicatable, ILoadUnload, IStartStop, IBeginProcessing,
        IHasSerialSections, ITimeLocal {


    public BridgeScSimpleAsPush(ISimpleEffect target) {
        super(target);
    }

    public void process(float[] input, int length, ISingleChanAudioSink sink) {
        target.process(input);
        if (input.length == length)
            sink.writeSamples(input);
        else if (length > 0) {
            // Last block. Truncate result
            float[] output = new float[length];
            System.arraycopy(input, 0, output, 0, length);
            sink.writeSamples(output);
        }
    }

    public BridgeScSimpleAsPush duplicate(IProcessingInfo info) {
        ISimpleEffect targetClone = PluginDuplicator.prepareCopy(target, info);
        return new BridgeScSimpleAsPush(targetClone);
    }

}
