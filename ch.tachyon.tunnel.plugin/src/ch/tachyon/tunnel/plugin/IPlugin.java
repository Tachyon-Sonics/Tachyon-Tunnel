package ch.tachyon.tunnel.plugin;

import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;

/**
 * This is the parent interface of the interface any plugin should implement.
 * <p>
 * A plugin should never implement this interface directly, but instead implement one of
 * the following subinterfaces, which defines how the processing is implemented:
 * <ul>
 * <li>{@link ISimpleEffect}
 * <li>{@link IMultiChanEffect}
 * <li>{@link IMultiChanPullEffect}
 * <li>{@link IMultiChanPushEffect}
 * </ul>
 * All the above interfaces extend this one, which is only a "marker" interface for plugins.
 * <p>
 * In addition, plugins can
 * <ul>
 * <li>Implement any additional interfaces such as {@link IStartStop}, {@link IFixedChunkLength}, {@link ITimeLocal}, etc
 * to provide or request additional functionality
 * <li>Use any annotation such as {@link Name}, {@link Range}, {@link MultiThreading}, etc to declare specific
 * functionalities or properties
 * </ul>
 * <p>
 * The following shows the typical life cycle of a plugin object:
 * (*optional method; +if corresponding interface is implemented):
 * <ul>
 * <li><code>new()</code>
 * <li>+*Retrieve static info ({@link ILicensed#getAuthor()}, etc; query of annotations on the class and/or getters)
 * <li>Zero or more times (each time the plugin is used):
 * <ul>
 * <li>+{@link ILoadUnload#load(IProcessingInfo)}
 * <li>Set/get zero or more parameter values using the getter and setters
 * <li>Zero or more times (for each processed audio file or each non-contiguous audio chunk):
 * <ul>
 * <li>+{@link IStartStop#startProcessing(IProcessingInfo)}
 * <li>*Invocation of zero or more of the following processing-related getter (in arbitrary order):
 * <ul>
 * <li>+*{@link ILatency#getLatency(ch.tachyon.tunnel.common.IoDirection)}
 * <li>+*{@link IChangeChannelCount#getNbOutputChannels(IProcessingInfo)}
 * <li>+*{@link IFixedChunkLength#getFixedChunkLength()}
 * <li>+*{@link IHasSerialSections#createSerialSections(ISerialSectionFactory)} and creation of working copies using
 * <code>clone()</code> or by creating a new instance and re-running the life-cycle (optionnal method may not be invoked again).
 *  {@link IHasSerialSections#setSerialSections(ISerialSectionPool)} is invoked on the working copies instead of
 *  {@link IHasSerialSections#createSerialSections(ISerialSectionFactory)}.
 * </ul>
 * <li>+{@link IBeginProcessing#beginProcessing(IProcessingInfo)}.
 * <li>one or more calls to <code>process()</code>. The process method is defined in the subinterfaces, such as
 * {@link ISimpleEffect#process(float[])}, {@link IPushEffect#process(float[], int, ch.tachyon.tunnel.common.ISingleChanAudioSink)}, etc.
 * <li>+{@link IStartStop#stopProcessing()}
 * </ul>
 * <li>+{@link ILoadUnload#unload()}
 * </ul>
 * <li>Instance no longer referenced. Note that it will only get GC'ed when the associated plugin bundle is released
 * </ul>
 */
public interface IPlugin {


}
