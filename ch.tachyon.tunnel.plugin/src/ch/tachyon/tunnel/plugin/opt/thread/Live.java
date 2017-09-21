package ch.tachyon.tunnel.plugin.opt.thread;

import java.lang.annotation.*;
import java.util.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;

/**
 * Indicate that the plugin is suitable for live performance. A "live performance" is a situation
 * in which both audio input and output are realtime, such as processing audio form the microphone
 * and playing the result in the loudspeaker. The condition a plugin must satisfy to have this
 * annotation is basically that it can cope with a given, fixed latency in a live performance.
 * <p>
 * More precisely, assuming the plugins processes audio chunks of constant size, it is suitable for live
 * performance if it satisfies all of the following during the processing:
 * <ul>
 * <li>It implements the {@link IMultiChanEffect} or {@link ISimpleEffect} interface
 * <li>The theoretical processing time does not change significantly over time
 * <li>The memory usage does not change significantly over time
 * <li>The amount of garbaged (objects elligible for gc) and created objects does not change significantly over time
 * <li>Garbaged and created objects are limited to small objects such as boxed primitives
 * (<code>java.lang.Integer</code>, <code>java.lang.Double</code>, etc) or {@link Iterator}s.
 * </ul>
 * where "not significantly" means in an order of magnitude that is less than about +/- 20%.
 * Note that absolute processing speed is not taken into account. The above conditions just ensures that
 * the plugin is either always fast enough or never fast enough on a given CPU and architecture, and with
 * given lateny constraints.
 * <p>
 * The above conditions also ensure a plugin can be properly tuned to run within hard latency constraints
 * on a Real-Time Java implementation.
 * In practice the last condition is quite efficient in preventing full (and slow) garbage collections
 * during the processing even in some standard Java runtimes, and to allow relatively low latencies.
 * <p>
 * These conditions do not need to be statisfied before {@link IBeginProcessing#beginProcessing(IProcessingInfo)}
 * or after {@link IStartStop#stopProcessing()} and inside of these two methods.
 * <p>
 * A plugin is not suitable for live performance for instance if it
 * <ul>
 * <li>Occasionally requires a more complex algorithm, depending on the signal (such as a click removal plugin)
 * <li>Has CPU and memory usage that depends on randomly generated numbers
 * <li>Create or destroy some objects only on some processed chunks
 * <li>Use a data structure or algorithm that has not a guaranted constant processing time (Notable examples are
 * {@link HashMap}, {@link HashSet} and {@link Hashtable}).
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Live {

}
