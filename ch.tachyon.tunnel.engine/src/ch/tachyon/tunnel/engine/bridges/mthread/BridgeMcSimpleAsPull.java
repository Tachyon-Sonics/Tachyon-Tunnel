package ch.tachyon.tunnel.engine.bridges.mthread;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;


public class BridgeMcSimpleAsPull extends BridgeMcSimpleBase implements IMultiChanPullEffect, IDuplicatable, ILoadUnload, IStartStop, IBeginProcessing,
        IHasSerialSections, ITimeLocal {

    public BridgeMcSimpleAsPull(IMultiChanEffect target) {
        super(target);
    }

    public int process(IMultiChanAudioSource source, float[][] data) {
        int amount = source.readSamples(data);
        if (amount > 0)
            target.process(data);
        return amount;
    }

    public BridgeMcSimpleAsPull duplicate(IProcessingInfo info) {
        IMultiChanEffect targetClone = PluginDuplicator.prepareCopy(target, info);
        return new BridgeMcSimpleAsPull(targetClone);
    }


}
