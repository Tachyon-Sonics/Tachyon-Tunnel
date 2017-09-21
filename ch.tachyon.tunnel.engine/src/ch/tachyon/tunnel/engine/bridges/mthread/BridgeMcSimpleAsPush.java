package ch.tachyon.tunnel.engine.bridges.mthread;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class BridgeMcSimpleAsPush extends BridgeMcSimpleBase implements IMultiChanPushEffect, IDuplicatable, ILoadUnload, IStartStop, IBeginProcessing,
        IHasSerialSections, ITimeLocal {

    public BridgeMcSimpleAsPush(IMultiChanEffect target) {
        super(target);
    }

    public void process(float[][] input, int length, IMultiChanAudioSink sink) {
        target.process(input);
        if (input[0].length == length)
            sink.writeSamples(input);
        else if (length > 0) {
            // Last block. Truncate result
            float[][] output = new float[input.length][length];
            for (int c = 0; c < input.length; c++)
                System.arraycopy(input[c], 0, output[c], 0, length);
            sink.writeSamples(output);
        }
    }

    public BridgeMcSimpleAsPush duplicate(IProcessingInfo info) {
        IMultiChanEffect targetClone = PluginDuplicator.prepareCopy(target, info);
        return new BridgeMcSimpleAsPush(targetClone);
    }

}
