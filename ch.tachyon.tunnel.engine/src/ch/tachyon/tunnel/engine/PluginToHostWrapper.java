package ch.tachyon.tunnel.engine;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.swing.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.engine.bridges.mthread.*;
import ch.tachyon.tunnel.engine.bridges.mthread.serial.*;
import ch.tachyon.tunnel.engine.gui.*;
import ch.tachyon.tunnel.engine.storage.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.engine.utils.bean.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * Base class for bridging a plugin ({@link IPlugin}) into an effect ({@link IEffect})
 * usable by a host.
 * <p>
 * Every subclass must have a two-args constructor accepting two instances of the {@link IPlugin}
 * <code>P</code> subclass and a {@link Properties}, and pass them to {@link #init(IPlugin, IPlugin, Class, IMtContext, Properties)}.
 * @param <P> the target (bridged) plugin interface
 */
public abstract class PluginToHostWrapper<P extends IPlugin> implements IEffect {

    /**
     * Chunk length to use if one is required and neither the host nor the plugin
     * have a preference.
     */
    protected final static int DEFAULT_CHUNK_LENGTH = 2048;

    protected final static int DEFAULT_MAX_LENGTH = 8192;

    /**
     * The bridged {@link IPlugin} to use for non-processing methods
     */
    protected IPlugin baseTarget;

    /**
     * The bridged {@link IPlugin} to use for processing (<code>process()</code>) method
     * and {@link IStartStop} methods.
     * <p>
     * This is either the same as {@link #baseTarget}, or a multi-threaded wrapper of it.
     */
    protected P processingTarget;

    private IMtContext mtContext;
    private Class<? extends IEffect> hostInterface;
    private Properties properties;
    private List<IParameter> parameters;

    private EffectDialog dialog = null;
    private ProcessingInfoBridge pluginInfo;
    private ch.tachyon.tunnel.host.IProcessingInfo hostInfo;
    private PluginStorage storage;
    private boolean hasBegun;
    private AtomicLong processedFrames = new AtomicLong();
    private boolean handleMonitoring;
    
    private boolean loaded = false;
    private boolean started = false;

    private float[][] inputs;
    private float[][] wrappedInputs;
    private int inputIndex;


    public abstract void init(IPlugin target, IPlugin processingTarget, IMtContext mtContext, Properties properties);

    /**
     * Create a new bridge for the given plugin into the given host interface.
     * <p>
     * The given plugin instances cas be accessed from the protected fields {@link #baseTarget}
     * and {@link #processingTarget}.
     * @param baseTarget the bridged plugin instance to use for non-processing methods
     * @param processingTarget the bridged plugin instance to use for processing (<code>process()</code>) method
     * and {@link IStartStop} methods. This is either the same as <code>baseTarget</code>, or a multi-threaded wrapper of it.
     * @param hostInterface the host interface to bridge the given plugin to
     * @param properties the properties associated with the plugin
     */
    @SuppressWarnings("unchecked")
    protected void init(IPlugin baseTarget, P processingTarget, Class<?> hostInterface, IMtContext mtContext, Properties properties) {
        this.baseTarget = baseTarget;
        this.processingTarget = processingTarget;
        this.hostInterface = (Class<? extends IEffect>) hostInterface;
        this.mtContext = mtContext;
        this.properties = properties;
    }

    public IPlugin getBaseTarget() {
        return baseTarget;
    }

    public P getProcessingTarget() {
        return processingTarget;
    }

    // Properties

    public String getName() {
        Name nameAnno = getAnnotation(Name.class);
        if (nameAnno != null)
            return nameAnno.value();
        return convertName(baseTarget.getClass().getSimpleName());
    }

    static String convertName(String name) {
        StringBuilder result = new StringBuilder();
        boolean upperCase = true;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch) && !upperCase) {
                result.append(' ');
                upperCase = true;
            } else
                upperCase = false;
            result.append(ch);
        }
        return result.toString();
    }

    public String getClassName() {
        return baseTarget.getClass().getName();
    }

    public Class<? extends IEffect> getInterface() {
        return hostInterface;
    }

    public String getCategory() {
        Category categoryAnno = getAnnotation(Category.class);
        if (categoryAnno != null)
            return categoryAnno.value();
        return null;
    }

    public List<IParameter> getParameters() {
        if (parameters == null)
            parameters = getParameters(baseTarget);
        return parameters;
    }

    private List<IParameter> getParameters(IPlugin target) {
        List<IParameter> result = new ArrayList<IParameter>();
        if (target instanceof IDynamicMetaData) {
            @SuppressWarnings("unchecked")
            List<ParameterInfo<IPlugin>> pInfos = ((IDynamicMetaData<IPlugin>) target).getParameters();
            for (ParameterInfo<IPlugin> pInfo : pInfos) {
                ParameterInfoParameter<IPlugin> param = new ParameterInfoParameter<IPlugin>(target, pInfo);
                result.add(param);
            }
        } else {
            ClassInfo info = new ClassInfo(target.getClass());
            for (MemberInfo property : info.getMembers()) {
                PropertyParameter<IPlugin> param = new PropertyParameter<IPlugin>(target, property, properties, property.getName());
                result.add(param);
            }
            Collections.sort(result, new Comparator<IParameter>() {

                public int compare(IParameter o1, IParameter o2) {
                    PropertyParameter<?> p1 = (PropertyParameter<?>) o1;
                    PropertyParameter<?> p2 = (PropertyParameter<?>) o2;
                    return p1.getOrder() - p2.getOrder();
                }
            });
        }
        return result;
    }

    public String getDescription() {
        Description descrAnno = getAnnotation(Description.class);
        if (descrAnno != null)
            return descrAnno.value();
        return null;
    }

    public <E extends Annotation> E getAnnotation(Class<E> annoType) {
        return AnnotationUtils.getAnnotation(baseTarget.getClass(), annoType, properties);
    }

    public Reader getDocumentation() {
        Documentation docAnno = getAnnotation(Documentation.class);
        if (docAnno != null) {
            InputStream stream = baseTarget.getClass().getResourceAsStream(docAnno.value());
            try {
                return new InputStreamReader(stream, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    // Life cycle

    public void load(ch.tachyon.tunnel.host.IProcessingInfo info) {
        if (loaded)
            throw new IllegalStateException("load() has already be invoked.");
        this.storage = new PluginStorage(baseTarget.getClass());
        this.hostInfo = info;
        this.pluginInfo = new ProcessingInfoBridge(info, storage, mtContext);
        if (baseTarget instanceof ILoadUnload)
            ((ILoadUnload) baseTarget).load(pluginInfo);
        setParameterDefaults();
        loaded = true;
    }

    private void setParameterDefaults() {
        for (IParameter parameter : getParameters()) {
            Range range = parameter.getRange();
            if (range != null) {
                Class<?> type = parameter.getType();
                double defValue = range.defaultValue();
                if (!Double.isNaN(defValue))
                    parameter.setInternalValue(DoubleConverter.fromDouble(defValue, type));
            }
        }
    }

    public JPanel createControlPanel() {
        checkLoaded();
        checkStarted(false);
        return new EffectControlPanel(this, hostInfo);
    }

    public void showGui(JFrame parent, IGuiListener listener) {
        checkLoaded();
        checkStarted(false);
        dialog = new EffectDialog(parent, this, hostInfo, listener);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void hideGui() {
        checkLoaded();
        checkStarted(false);
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
    }

    public <E, T> void setParameterValue(ParameterDefinition<E, T> parameter, T value) {
        checkLoaded();
        setParameterValue(parameter.getName(), value);
    }

    @SuppressWarnings("unchecked")
    public <E, T> T getParameterValue(ParameterDefinition<E, T> parameter) {
        checkLoaded();
        return (T) getParameterValue(parameter.getName());
    }

    public void setParameterValue(String name, Object value) {
        checkLoaded();
        // Try with Java name first
        for (IParameter param : getParameters()) {
            if (param.getName().equals(name)) {
                param.setValue(value, hostInfo);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown parameter \"" + name + "\"");
    }

    public Object getParameterValue(String name) {
        checkLoaded();
        for (IParameter param : getParameters()) {
            if (param.getName().equals(name))
                return param.getValue(hostInfo);
        }
        throw new IllegalArgumentException("Unknown parameter \"" + name + "\"");
    }

    public int getPreferredChunkLength() {
        checkStarted(true);
        if (baseTarget instanceof IFixedChunkLength)
            return pluginInfo.adaptPluginLength(((IFixedChunkLength) baseTarget).getFixedChunkLength());
        return -1;
    }

    protected int getActualMaxChunkLength(ch.tachyon.tunnel.host.IProcessingInfo info) {
        int result = info.getMaxChunkLength(getActualChunkLength());
        if (result < 0)
            result = DEFAULT_MAX_LENGTH;
        return result;
    }

    /**
     * Get the chunk length. This is either the desired plugin's chunk length, or a default
     * value if the plugin has no preference. This method never returns -1.
     */
    protected int getActualChunkLength() {
        int result = getPreferredChunkLength();
        if (result <= 0)
            result = hostInfo.getHostPreferredChunkLength();
        if (result <= 0)
            result = DEFAULT_CHUNK_LENGTH;
        return result;
    }
    
    private void checkStarted(boolean value) {
        if (value) {
            assert started : "This method can only be invoked bewteen startProcessing() and stopProcessing()";
        } else {
            assert !started : "This method cannot be invoked bewteen startProcessing() and stopProcessing()";
        }
    }
    
    private void checkLoaded() {
        assert loaded : "This method can only be invoked between load() and unload()";
    }

    public void startProcessing(final ch.tachyon.tunnel.host.IProcessingInfo info) {
        if (!loaded)
            throw new IllegalStateException("This plugin has not been loaded(). Please call load() first!");
        if (started)
            throw new IllegalStateException("This plugin has already been started. You may want to call stopProcessing() first to restart it");
        if (info != hostInfo) {
            this.hostInfo = info;
            this.pluginInfo = new ProcessingInfoBridge(info, storage, mtContext);
        }
        storage.createChannelStorage();
        if (processingTarget instanceof IStartStop)
            ((IStartStop) processingTarget).startProcessing(pluginInfo);
        // Check if IHasSerialSection is implemented, but not processed.
        // If that is the case, it means the plugin was NOT wrapped into a multi-threaded version.
        // We hence need to supply dummy implementation (sufficient for single-threading to work)
        if (!(processingTarget instanceof ISerialSectionHandler) && baseTarget instanceof IHasSerialSections) {
//            MultiThreading mtAnno = baseTarget.getClass().getAnnotation(MultiThreading.class);
//            if (mtAnno == null && getClass().desiredAssertionStatus()) {
//                // Warn if IHasSerialSection is implemented directly
//                Class<?>[] intfs = baseTarget.getClass().getInterfaces();
//                boolean hasIt = false;
//                for (Class<?> intf : intfs) {
//                    if (intf.equals(IHasSerialSections.class))
//                        hasIt = true;
//                }
//                if (hasIt)
//                    Debug.warn("Plugin " + baseTarget.getClass() + " implements IHasSerialSection, but lacks the @MultiThreading annotation." +
//                    		"\nThe engine will supply dummy ISerialSection implementations to make it work. However, you way want to check if this makes sense.");
//            }
            final IMtContext mtContext = new DummyMtContext();
            // Create dummy serial sections
            ((IHasSerialSections) baseTarget).createSerialSections(new ISerialSectionFactory() {

                public void ensureMinInputHistorySize(int value) {
                    // Wow. We have to honor that...
                    inputs = new float[value * info.getNumberOfChannels()][];
                    inputIndex = 0;
                }

                public ISerialSection createSerialSection(String name, Object userData) {
                    return new DummySerialSection(name, userData);
                }

                public IProcessingInfo getProcessingInfo() {
                    return pluginInfo;
                }

                public IMtContext getMtContext() {
                    return mtContext;
                }

            });
        }
        hasBegun = false;
        processedFrames.set(0);
        handleMonitoring = true;
        if (processingTarget instanceof IProcessedFramesMonitor) {
            // Delegate monitoring to plugin
            ((IProcessedFramesMonitor) processingTarget).setCounter(processedFrames);
            handleMonitoring = false;
        }
        started = true;
    }
    
    public boolean isParallelized() {
        if (processingTarget instanceof MtBase<?>)
            return ((MtBase<?>) processingTarget).isParallelized();
        return false;
    }

    protected int getLatency(IoDirection ioDirection) {
        checkStarted(true);
        if (baseTarget instanceof ILatency)
            return (int) ( ((ILatency) baseTarget).getLatency(ioDirection) + 0.5); // TODO [tt] review
        return 0;
    }

    public long getAdditionalInputFrames() {
        checkStarted(true);
        return getLatency(IoDirection.INPUT);
    }

    public boolean requiresSamplesBeforeOrAfter() {
        return (baseTarget instanceof IRequiredBeforeAfter);
    }

    public long getRequiredSamplesBefore() {
        checkStarted(true);
        if (baseTarget instanceof IRequiredBeforeAfter)
            return ((IRequiredBeforeAfter) baseTarget).getRequiredSamplesBefore(pluginInfo);
        return 0;
    }

    public long getRequiredSamplesAfter() {
        checkStarted(true);
        if (baseTarget instanceof IRequiredBeforeAfter)
            return ((IRequiredBeforeAfter) baseTarget).getRequiredSamplesAfter(pluginInfo);
        return 0;
    }

    public double getTimeChange() {
        checkStarted(true);
        int latencyIn = getLatency(IoDirection.INPUT);
        int latencyOut = getLatency(IoDirection.OUTPUT);
        if (latencyIn == 0)
            return 1.0;
        return (double) latencyOut / (double) latencyIn;
    }

    public void beginProcessing(ch.tachyon.tunnel.host.IProcessingInfo info) {
        checkStarted(true);
        if (info != hostInfo) {
            hostInfo = info;
            pluginInfo = new ProcessingInfoBridge(info, storage, mtContext);
        }
        if (processingTarget instanceof IBeginProcessing)
            ((IBeginProcessing) processingTarget).beginProcessing(pluginInfo);
        hasBegun = true;
    }

    protected void beginIfWorth() {
        if (!hasBegun)
            beginProcessing(hostInfo);
    }

    /**
     * Wrap an input buffer so it complies with the contract of {@link IHasSerialSections}, if implemented.
     * <p>
     * In particular, a plugin can request through {@link ISerialSectionFactory#ensureMinInputHistorySize(int)}
     * that a different input buffer is supplied for every given number of consecutive audio chunks.
     */
    protected float[] wrapInput(float[] input) {
        if (inputs != null) {
            if (inputs[inputIndex] == null || inputs[inputIndex].length != input.length)
                inputs[inputIndex] = new float[input.length];
            System.arraycopy(input, 0, inputs[inputIndex], 0, input.length);
            input = inputs[inputIndex];
            inputIndex = (inputIndex + 1) % inputs.length;
        }
        return input;
    }

    protected float[][] wrapInput(float[][] input) {
        if (inputs == null)
            return input;
        if (wrappedInputs == null || wrappedInputs.length != input.length)
            wrappedInputs = new float[input.length][];
        for (int c = 0; c < input.length; c++)
            wrappedInputs[c] = wrapInput(input[c]);
        return wrappedInputs;
    }
    
    protected boolean isSane(float[][] data, boolean in) {
        assert data.length > 0 : "Illegal zero-channel data. At least one channel is required";
        int nbFrames = 0;
        for (int chan = 0; chan < data.length; chan++) {
            if (chan == 0)
                nbFrames = data[chan].length;
            else
                assert nbFrames == data[chan].length : "Illegal data: number of samples does not match in all channels";
            assert isSane(data[chan], in);
        }
        return true;
    }

    protected boolean isSane(float[] data, boolean in) {
        for (float v : data) {
            if (Float.isInfinite(v) || Float.isNaN(v)) {
                if (in)
                    assert false : "Infinite or NaN value supplied by the host";
                else
                    assert false : "Infinite or NaN value produced by the plugin";
                return false;
            }
        }
        return true;
    }

    protected void unwrapInput(float[] wrapped, float[] initial) {
        if (wrapped != initial)
            System.arraycopy(wrapped, 0, initial, 0, wrapped.length);
    }

    protected void processed(long nbFrames) {
        if (handleMonitoring)
            processedFrames.addAndGet(nbFrames);
    }

    public long getProcessedFrames() {
        return processedFrames.get();
    }

    public void stopProcessing() {
        if (!started)
            throw new IllegalStateException("stopProcessing() invoked without a matching call to startProcessing()");
        if (processingTarget instanceof IStartStop)
            ((IStartStop) processingTarget).stopProcessing();
        inputs = null;
        wrappedInputs = null;
        storage.destroyChannelStorage();
        started = false;
    }

    public void unload() {
        if (!loaded)
            throw new IllegalStateException("unload() invoked without a matching load() call");
        if (baseTarget instanceof ILoadUnload)
            ((ILoadUnload) baseTarget).unload();
        this.hostInfo = null;
        this.pluginInfo = null;
        this.storage = null;
        loaded = false;
    }

    protected ch.tachyon.tunnel.host.IProcessingInfo getHostInfo() {
        return this.hostInfo;
    }

    protected IProcessingInfo getPluginInfo() {
        return this.pluginInfo;
    }

    // Helpers

    protected boolean needsRebuffering() {
        return needsRebuffering(true);
    }
    
    protected boolean needsRebuffering(boolean latencyCorrection) {
        if (latencyCorrection) {
            if (getLatency(IoDirection.INPUT) > 0 || getLatency(IoDirection.OUTPUT) > 0)
                return true;
        }
        int pluginChunkSize = getPreferredChunkLength();
        if (pluginChunkSize <= 0)
            return false;
        return pluginChunkSize != hostInfo.negociateFixedChunkLength(pluginChunkSize);
    }

    @Override
    public String toString() {
        return "IEffect Wrapper [name=" + getName()
                + ",class=" + baseTarget.getClass().getName()
                + ",interface=" + hostInterface.getSimpleName();
    }

}
