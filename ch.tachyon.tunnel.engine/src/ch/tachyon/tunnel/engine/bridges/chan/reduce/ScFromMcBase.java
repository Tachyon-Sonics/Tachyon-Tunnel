package ch.tachyon.tunnel.engine.bridges.chan.reduce;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;

public abstract class ScFromMcBase<SC extends IEffect, MC extends IEffect> implements IEffect {

    protected MC target;
    protected float[][] mcSamples;
    
    
    public void init(MC target) {
        this.target = target;
    }
    
    public String getName() {
        return target.getName();
    }

    public String getCategory() {
        return target.getCategory();
    }

    public String getClassName() {
        return target.getClassName();
    }

    public abstract Class<? extends IEffect> getInterface();

    public List<IParameter> getParameters() {
        return target.getParameters();
    }

    public String getDescription() {
        return target.getDescription();
    }

    public Reader getDocumentation() {
        return target.getDocumentation();
    }

    public <E extends Annotation> E getAnnotation(Class<E> annoType) {
        return target.getAnnotation(annoType);
    }

    public void load(IProcessingInfo info) {
        if (info.getNumberOfChannels() != 1)
            throw new IllegalArgumentException();
        target.load(info);
    }

    public Object createControlPanel() {
        return target.createControlPanel();
    }

    public void showGui(Object parent, IGuiListener listener) {
        target.showGui(parent, listener);
    }

    public void hideGui() {
        target.hideGui();
    }

    public <P, T> void setParameterValue(ParameterDefinition<P, T> parameter, T value) {
        target.setParameterValue(parameter, value);
    }

    public <P, T> T getParameterValue(ParameterDefinition<P, T> parameter) {
        return target.getParameterValue(parameter);
    }

    public void setParameterValue(String name, Object value) {
        target.setParameterValue(name, value);
    }

    public Object getParameterValue(String name) {
        return target.getParameterValue(name);
    }

    public int getPreferredChunkLength() {
        return target.getPreferredChunkLength();
    }

    public void startProcessing(IProcessingInfo info) {
        if (info.getNumberOfChannels() != 1)
            throw new IllegalArgumentException();
        target.startProcessing(info);
        mcSamples = new float[1][];
    }

    public long getAdditionalInputFrames() {
        return target.getAdditionalInputFrames();
    }

    public boolean requiresSamplesBeforeOrAfter() {
        return target.requiresSamplesBeforeOrAfter();
    }

    public long getRequiredSamplesBefore() {
        return target.getRequiredSamplesBefore();
    }

    public long getRequiredSamplesAfter() {
        return target.getRequiredSamplesAfter();
    }

    public double getTimeChange() {
        return target.getTimeChange();
    }

    public long getProcessedFrames() {
        return target.getProcessedFrames();
    }

    public void stopProcessing() {
        target.stopProcessing();
        mcSamples = null;
    }

    public void unload() {
        target.unload();
    }

}
