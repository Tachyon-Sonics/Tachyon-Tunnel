package ch.tachyon.tunnel.tools;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import javax.swing.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;

/**
 * Wrapper around {@link IPullEffect} that allows setting parameters before the effect is loaded.
 * <p>
 * Parameters set before the effect is loaded are stored, and get "transmitted" to the effect as soon as it is loaded
 */
class LazyPullEffect implements IPullEffect {

    private final IPullEffect target;
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private boolean loaded = false;


    public LazyPullEffect(IPullEffect target) {
        this.target = target;
    }

    @Override
    public boolean canWriteFasterThanRead() {
        return target.canWriteFasterThanRead();
    }

    @Override
    public int process(IMultiChanAudioSource source, float[][] output) {
        return target.process(source, output);
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public String getCategory() {
        return target.getCategory();
    }

    @Override
    public String getClassName() {
        return target.getClassName();
    }

    @Override
    public Class<? extends IEffect> getInterface() {
        return target.getInterface();
    }

    @Override
    public List<IParameter> getParameters() {
        return target.getParameters();
    }

    @Override
    public String getDescription() {
        return target.getDescription();
    }

    @Override
    public Reader getDocumentation() {
        return target.getDocumentation();
    }

    @Override
    public <E extends Annotation> E getAnnotation(Class<E> annoType) {
        return target.getAnnotation(annoType);
    }

    @Override
    public void load(IProcessingInfo info) {
        target.load(info);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            target.setParameterValue(name, value);
        }
        parameters.clear();
        loaded = true;
    }

    @Override
    public JPanel createControlPanel() {
        return target.createControlPanel();
    }

    @Override
    public void showGui(JFrame parent, IGuiListener listener) {
        target.showGui(parent, listener);
    }

    @Override
    public void hideGui() {
        target.hideGui();
    }

    @Override
    public <P, T> void setParameterValue(ParameterDefinition<P, T> parameter, T value) {
        target.setParameterValue(parameter, value);
    }

    @Override
    public <P, T> T getParameterValue(ParameterDefinition<P, T> parameter) {
        return target.getParameterValue(parameter);
    }

    @Override
    public void setParameterValue(String name, Object value) {
        if (loaded)
            target.setParameterValue(name, value);
        else
            parameters.put(name, value);
    }

    @Override
    public Object getParameterValue(String name) {
        if (parameters.containsKey(name))
            return parameters.get(name);
        return target.getParameterValue(name);
    }

    @Override
    public int getPreferredChunkLength() {
        return target.getPreferredChunkLength();
    }

    @Override
    public void startProcessing(IProcessingInfo info) {
        target.startProcessing(info);
    }

    @Override
    public long getAdditionalInputFrames() {
        return target.getAdditionalInputFrames();
    }

    @Override
    public boolean requiresSamplesBeforeOrAfter() {
        return target.requiresSamplesBeforeOrAfter();
    }

    @Override
    public long getRequiredSamplesBefore() {
        return target.getRequiredSamplesBefore();
    }

    @Override
    public long getRequiredSamplesAfter() {
        return target.getRequiredSamplesAfter();
    }

    @Override
    public float getTimeChange() {
        return target.getTimeChange();
    }

    @Override
    public long getProcessedFrames() {
        return target.getProcessedFrames();
    }

    @Override
    public void stopProcessing() {
        target.stopProcessing();
    }

    @Override
    public void unload() {
        target.unload();
    }

}
