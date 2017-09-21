package ch.tachyon.tunnel.engine.bridges.mthread;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

public class BridgeMcSimpleBase implements ILoadUnload, IStartStop, IBeginProcessing, IHasSerialSections, ITimeLocal, IFixedChunkLength {

    protected final IMultiChanEffect target;


    public BridgeMcSimpleBase(IMultiChanEffect target) {
        this.target = target;
    }

    public boolean canWriteFasterThanRead() {
        return false;
    }

    public void load(IProcessingInfo info) {
        if (target instanceof ILoadUnload)
            ((ILoadUnload) target).load(info);
    }

    public void startProcessing(IProcessingInfo info) {
        if (target instanceof IStartStop)
            ((IStartStop) target).startProcessing(info);
    }

    public int getFixedChunkLength() {
        if (target instanceof IFixedChunkLength)
            return ((IFixedChunkLength) target).getFixedChunkLength();
        return -1;
    }

    public int getRequiredFramesBefore(IProcessingInfo info) {
        if (target instanceof ITimeLocal)
            return ((ITimeLocal) target).getRequiredFramesBefore(info);
        return 0;
    }

    public void createSerialSections(ISerialSectionFactory factory) {
        if (target instanceof IHasSerialSections)
            ((IHasSerialSections) target).createSerialSections(factory);
    }

    public void setSerialSections(ISerialSectionPool pool) {
        if (target instanceof IHasSerialSections)
            ((IHasSerialSections) target).setSerialSections(pool);
    }

    public void beginProcessing(IProcessingInfo info) {
        if (target instanceof IBeginProcessing)
            ((IBeginProcessing) target).beginProcessing(info);
    }

    public void stopProcessing() {
        if (target instanceof IStartStop)
            ((IStartStop) target).stopProcessing();
    }

    public void unload() {
        if (target instanceof ILoadUnload)
            ((ILoadUnload) target).unload();
    }

}
