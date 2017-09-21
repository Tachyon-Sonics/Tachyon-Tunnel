package ch.tachyon.tunnel.host.effect;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;

import javax.swing.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;

// todo continue with all stuff from all possible interfaces
/**
 * Base interface for accessing and using audio plugins from a host application.
 * <p>
 * In general this interface is not used directly, but one of the sub-interfaces, such as
 * <ul>
 * <li>{@link ISingleChanEffect}
 * <li>{@link ISimpleEffect}
 * <li>{@link IPullEffect}
 * <li>etc
 * </ul>
 */
public interface IEffect {

    /**
     * Get the name of this effect.
     * <p>
     * The returned value can be used to display the effect in a GUI.
     * <p>
     * The returned name is not guaranted to be unique within a {@link IPluginBundle}.
     * {@link #getClassName()} should be used to identify plugins uniquely. The returned name
     * is translated according to the current {@link Locale} if available.
     * @return the effect name
     */
    public String getName();

    /**
     * @return the category (and optional subcategories, separated by ":") in which this effect is best
     * classified. Return <code>null</code> if the effect does not provide any category.
     * <p>
     * The category name is not translated to the current {@link Locale}. It should correspond to one of the
     * values documented in {@link Category}, or be <code>null</code>.
     */
    public String getCategory();

    /**
     * Get the class name that can be passed to {@link IPluginBundle#loadPlugin(Class, String)} (second
     * argument) to reload the same plugin later.
     * <p>
     * This method must be used in place of {@link #getClass() this.getClass()} because implementations
     * of this interface are most of the time wrapper classes rather than the actual plugin class.
     * <p>
     * This method must be used in place of {@link #getName()} to identify effects in a unique may
     * within a {@link IPluginBundle}. To identify effects in a unique way between multiple bundles,
     * {@link IPluginBundle#getUrlString()} and this method can be combined.
     * @see #getInterface()
     */
    public String getClassName();

    /**
     * The plugin's processing interface, such as {@link ISimpleEffect}, {@link IPushEffect}, etc.
     * <p>
     * The result can be passed to {@link IPluginBundle#loadPlugin(Class, String)} (first argument)
     * to reload the same plugin later.
     * @see IEffect#getClassName()
     */
    public Class<? extends IEffect> getInterface();

    /**
     * Get a list of parameters for this effect
     */
    public List<IParameter> getParameters();

    /**
     * Get a short description of this effect. This is either a single line or multiple lines
     * separated by "\n". In the later case the host may choose to display only the first line
     * or all lines.
     * <p>
     * The result is translated to the current {@link Locale} if available.
     * @return a short description
     */
    public String getDescription();

    /**
     * Get a {@link Reader} that allows you to get the plugin's full documentation.
     * The document can be displayed as a part of the application's help.
     * <p>
     * If you invoke this method, you must also close the returned <code>Reader</code> object after you have
     * finished reading from it.
     * <p>
     * The documentation is given as an XHTML document with UTF-8 encoding.
     * <p>
     * The document may contain different kinds of references (links):
     * <ul>
     * <li>A link starting with "<code>@</code>" followed by a fully qualified class name of a plugin is used to
     * link to the documentation of another plugin.
     * <li>A link starting with "<code>/</code>" is used to link to an HTML document that can be loaded using
     * {@link IPluginBundle#getClassLoader(boolean)}.{@link ClassLoader#getResourceAsStream(String) getResourceAsStream()}.
     * <li>A link without protocol (such as <code>http://</code>) is similar to the previous case, but is relative
     * to the current plugin or document.
     * <li>A link starting with a protocol (such as <code>http://</code>) is an external link to an internet page.
     * </ul>
     */
    public Reader getDocumentation();

    public <E extends Annotation> E getAnnotation(Class<E> annoType);

    /**
     * Load the given effect and prepare it so that parameters can be modified or the GUI can be shown.
     * <p>
     * This method must be invoked before playing with the parameters or the GUI, and before processing.
     * A call to {@link #load(IProcessingInfo)} must be matched with a call to {@link #unload()}.
     * @param info processing information. The relevant properties are {@link IProcessingInfo#getNumberOfChannels()},
     * {@link IProcessingInfo#getQuantization()} and {@link IProcessingInfo#getSampleRate()}. The other properties
     * do not need to return relevant values at this stage.
     */
    public void load(IProcessingInfo info);

    public JPanel createControlPanel();

    public void showGui(JFrame parent, IGuiListener listener);

    public void hideGui();

    public <P, T> void setParameterValue(ParameterDefinition<P, T> parameter, T value);

    public <P, T> T getParameterValue(ParameterDefinition<P, T> parameter);

    public void setParameterValue(String name, Object value);

    public Object getParameterValue(String name);

    /**
     * The preferred audio chunk size this plugin want to process, or <code>-1</code> if there is no
     * preferred chunk size. In general, a non-negative value is returned if that value would lead to
     * optimal processing latency and/or processing speed.
     * <p>
     * The host may or may not honor the returned size. If the size is not honored, the engine will
     * automatically rebuffer the audio chunks to the given size. This may lead to additional latency.
     * <p>
     * If the host honors the returned size, it <b>must</b> pass an {@link IProcessingInfo} implementation
     * (to {@link #startProcessing(IProcessingInfo)}) whose {@link IProcessingInfo#negociateFixedChunkLength(int)}
     * returns that size. This will inform the engine that it does not need to rebuffer the audio chunks.
     */
    public int getPreferredChunkLength();

    public void startProcessing(IProcessingInfo info);

    /**
     * Get the number of additional input frames that must be included for any processing.
     * <p>
     * The returned value is only useful for monitoring progress. For instance, when processing
     * an audio file or chunk of N frames (sample x channel), the progress will be given by<br>
     * <code>{@link #getProcessedFrames()} / (N + {@link #getAdditionalInputFrames()})</code>.
     * <p>
     * The returned value if usually non-zero when the plugin has latency that is being compensated.
     * Note that unless you use the {@link ISimpleEffect} interface, latency is automatically compensated
     * by the engine. If you use {@link ISimpleEffect}, you must use {@link ISimpleEffect#getLatency()}
     * to get the latency. This method should only be used with {@link #getProcessedFrames()} for
     * monitoring progress.
     * <p>
     * This method must be invoked just after {@link #startProcessing(IProcessingInfo)}.
     * The returned value might depend on the values of the plugin's parameters.
     */
    public long getAdditionalInputFrames();
    
    /**
     * Whether {@link #getRequiredSamplesBefore()} or {@link #getRequiredSamplesAfter()} might return
     * non zero. Unlike the two above methods, this one can be invoked before {@link #startProcessing(IProcessingInfo)}.
     */
    public boolean requiresSamplesBeforeOrAfter();

    public long getRequiredSamplesBefore();

    public long getRequiredSamplesAfter();

    /**
     * Get the time change of this effect. An effect that does not affect the speed or duration
     * returns <tt>1.0f</tt>. If the effect changes the duration, the returned value is the ratio
     * between the output duration and the input duration. A value smaller than <tt>1.0f</tt> means
     * that the output will be shorter than the input. A value greater than <tt>1.0f</tt> means
     * that the output will be longer than the input.
     * <p>
     * Most effects return <tt>1.0f</tt>. Exceptions are resampling and time-stretching effects for example.
     * <p>
     * This method can only be invoked after the parameters have been set, and after {@link IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}
     * has been invoked. The result can depend on the parameters values.
     * @return output length / input length
     */
    public float getTimeChange();

    /**
     * Return the approximate number of processed frames since the last call to {@link #startProcessing(IProcessingInfo)}.
     * <p>
     * A <i>frame</i> is a sample in every channel. In mono, frame is similar to sample. In stereo a frame consists of two samples,
     * one for each channel.
     * <p>
     * This method can be used to monitor processing progress. It is usually more accurate than relying on the callbacks
     * or number of calls to <code>process()</code>, especially when multicore processing is used. The exact instants at which
     * the value is updated are, however, not defined, and the host should not rely on it. The returned value is only meant as
     * a way to monitor the progress (for instance to display a progress bar). Note that you may certainly also need to use
     * {@link #getAdditionalInputFrames()}!
     * <p>
     * It is safe to invoke this method concurrently by another thread than the one that calls the <code>process()</code> method.
     * This method is very fast (it consists in an atomic read operation) and can be invoked multiple times during processing.
     * <p>
     * Note that if the plugin alters the audio length (resampling, time strecthing, etc), the returned value is based on the number
     * of input frames read rather than on the output frames produced.
     * @see #getAdditionalInputFrames()
     */
    public long getProcessedFrames();

    public void stopProcessing();

    public void unload();

}
