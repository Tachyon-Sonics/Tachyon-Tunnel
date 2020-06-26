package ch.tachyon.tunnel.engine.bridges.chan.expand;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;

import javax.swing.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.bridges.mthread.*;
import ch.tachyon.tunnel.engine.utils.concurrent.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;


public abstract class McFromScBase<MC extends IEffect, SC extends IEffect> implements IEffect {
    
    protected ThreadPool pool;
    protected SC descTarget;
    private IScEffectFactory<SC> scFactory;
    protected int nbChans;
    protected List<SC> targets;
    private boolean midSide;

    
    public void init(SC descTarget, IScEffectFactory<SC> scFactory) {
        this.descTarget = descTarget;
        this.scFactory = scFactory;
        this.midSide = (descTarget.getAnnotation(MidSide.class) != null);
    }
    
    protected void preProcessMidSide(float[][] data) {
        if (!midSide || data.length < 2)
            return;
        if (data.length == 2) {
            float[] left = data[0];
            float[] right = data[1];
            preProcessStereo(left, right);
        } else {
            int nbChans = data.length;
            for (int i = 0; i < nbChans - 1; i += 2)
                preProcessStereo(data[i], data[i + 1]);
            for (int i = 0; i < nbChans - 2; i++)
                preProcessStereo(data[i], data[i + 2]);
        }
    }

    private void preProcessStereo(float[] left, float[] right) {
        assert left.length == right.length;
        for (int i = 0; i < left.length; i++) {
            float l = left[i];
            float r = right[i];
            left[i] = (l + r) / 2.0f;
            right[i] = (l - r) / 2.0f;
        }
    }
    
    protected void postProcessMidSide(float[][] data) {
        if (!midSide || data.length < 2)
            return;
        if (data.length == 2) {
            float[] left = data[0];
            float[] right = data[1];
            postProcessStereo(left, right);
        } else {
            int nbChans = data.length;
            for (int i = nbChans - 3; i >= 0; i--)
                postProcessStereo(data[i], data[i + 2]);
            for (int i = 0; i < nbChans - 1; i += 2)
                postProcessStereo(data[i], data[i + 1]);
        }
    }

    private void postProcessStereo(float[] left, float[] right) {
        assert left.length == right.length;
        for (int i = 0; i < left.length; i++) {
            float m = left[i];
            float s = right[i];
            left[i] = m + s;
            right[i] = m - s;
        }
    }
    
    public String getName() {
        return descTarget.getName();
    }

    public String getCategory() {
        return descTarget.getCategory();
    }

    public String getClassName() {
        return descTarget.getClassName();
    }

    public abstract Class<? extends IEffect> getInterface();

    public List<IParameter> getParameters() {
        return descTarget.getParameters();
    }

    public String getDescription() {
        return descTarget.getDescription();
    }

    public Reader getDocumentation() {
        return descTarget.getDocumentation();
    }

    public <E extends Annotation> E getAnnotation(Class<E> annoType) {
        return descTarget.getAnnotation(annoType);
    }
    
    private static IProcessingInfo wrap(IProcessingInfo info, int chanIndex) {
        return new OneChanProcessingInfo(info, chanIndex, info.getNumberOfChannels());
    }
    
    public void load(IProcessingInfo info) {
        descTarget.load(wrap(info, 0));
        this.nbChans = info.getNumberOfChannels();
        if (nbChans <= 0)
            throw new IllegalArgumentException("Invalid number of channels: " + nbChans);
        this.targets = new ArrayList<SC>(nbChans); 
        targets.add(descTarget);
        for (int chan = 1; chan < nbChans; chan++) {
            SC chanTarget = scFactory.create(descTarget, info);
            chanTarget.load(wrap(info, chan));
            targets.add(chanTarget);
        }
    }

    public JPanel createControlPanel() {
        return descTarget.createControlPanel();
    }

    public void showGui(JFrame parent, IGuiListener listener) {
        descTarget.showGui(parent, listener);
    }

    public void hideGui() {
        descTarget.hideGui();
    }

    public <P, T> void setParameterValue(ParameterDefinition<P, T> parameter, T value) {
        for (SC target : targets)
            target.setParameterValue(parameter, value);
    }

    public <P, T> T getParameterValue(ParameterDefinition<P, T> parameter) {
        return descTarget.getParameterValue(parameter);
    }

    public void setParameterValue(String name, Object value) {
        for (SC target : targets)
            target.setParameterValue(name, value);
    }

    public Object getParameterValue(String name) {
        return descTarget.getParameterValue(name);
    }

    public int getPreferredChunkLength() {
        return descTarget.getPreferredChunkLength();
    }

    public void startProcessing(IProcessingInfo info) {
        for (int chan = 1; chan < nbChans; chan++) {
            // Copy parameters
            SC chanTarget = targets.get(chan);
            List<IParameter> srcParameters = descTarget.getParameters();
            List<IParameter> dstParameters = chanTarget.getParameters();
            assert srcParameters.size() == dstParameters.size() : "Target and its copy do not have the same number of parameters";
            for (int i = 0; i < srcParameters.size(); i++) {
                assert srcParameters.get(i).getName().equals(dstParameters.get(i).getName()) : "Target parameters do not match copy parameters";
                dstParameters.get(i).setInternalValue(srcParameters.get(i).getInternalValue());
            }
        }
        for (int chan = 0; chan < nbChans; chan++)
            targets.get(chan).startProcessing(wrap(info, chan));
        // Do not use a pool for monophonic audio, or if there is only one core
        MtContext mtContext = new MtContext();
        if (mtContext.getNbThreads() > 1 && nbChans > 1)
            pool = new ThreadPool("Channel", Math.min(nbChans, mtContext.getNbThreads()));
    }

    public long getAdditionalInputFrames() {
        return descTarget.getAdditionalInputFrames();
    }

    public boolean requiresSamplesBeforeOrAfter() {
        return descTarget.requiresSamplesBeforeOrAfter();
    }

    public long getRequiredSamplesBefore() {
        return descTarget.getRequiredSamplesBefore();
    }

    public long getRequiredSamplesAfter() {
        return descTarget.getRequiredSamplesAfter();
    }

    public double getTimeChange() {
        return descTarget.getTimeChange();
    }

    public long getProcessedFrames() {
        return descTarget.getProcessedFrames();
    }

    public void stopProcessing() {
        for (SC target : targets)
            target.stopProcessing();
        if (pool != null)
            pool.dispose();
        pool = null;
    }

    public void unload() {
        for (SC target : targets)
            target.unload();
        targets = null;
        nbChans = 0;
    }

}
