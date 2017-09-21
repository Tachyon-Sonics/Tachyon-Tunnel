package ch.tachyon.tunnel.plugin.opt.thread;

import java.lang.annotation.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;

/**
 * Indicate that this plugin supports multi-threading. It informs the engine that differents channels
 * and different audio chunks can be processed concurrently by different threads when running on
 * a multi-core machine.
 * <p>
 * Multi-threaded processing is implemented by using multiple instances of the plugin. Each instance is hence
 * single threaded. Note however that method calls on a single instance may not all originate
 * from the same thread, it is just guaranted that no method call will be concurrent on the
 * same instance.
 * <p>
 * When this annotation is present, the engine may:
 * <ul>
 * <li>Use one instance per channel to process the channels concurrently
 * <li>Unless {@link #markov()} is <code>true</code>, create a pool of instances to process
 * different chunks of audio concurrently.
 * </ul>
 * <p>
 * To allow the engine to create additional instances in order to process audio in parallel, the
 * plugin must ensure at least one of the following:
 * <ul>
 * <li>Expose all its parameters through getter and setters
 * <li>Implement the {@link Cloneable} interface (or the better {@link IPublicCloneable} subinterface).
 * <li>Implement the {@link IDuplicatable} interface.
 * </ul>
 * In the first case, the engine will create a new instance, and copy the parameters from the
 * old one to the new one. Note that the parameters themselves are not cloned.
 * <p>
 * In the second case, no copy of the parameters is done, your <code>clone()</code> method
 * implementation is responsible for copying the parameters. Same for the third case.
 * <p>
 * It is possible to put this annotation on a plugin that is not fully concurrent as long
 * as one of the following is done:
 * <ul>
 * <li>Serial sections are manually serialized in the code by implementing {@link IHasSerialSections}
 * and using {@link ISerialSection} objects, or by using Java synchronization primitives and classes.
 * <li>The {@link ITimeLocal} interface is implemented to inform that the plugin requires some samples
 * before and after the chunk to ensure locality
 * </ul>
 * In other words, this annotation means to the engine that it can treat the plugin as fully concurrent;
 * and that critical sections, if any, are handled manually by the plugin itself.
 * <p>
 * This annotations is only supported together with the following plugin interfaces, and with the
 * following restrictions:
 * <ul>
 * <li>{@link ISimpleEffect} - no restriction
 * <li>{@link IPushEffect} - no restriction. Calls to {@link ISingleChanAudioSink#writeSamples(float[])}
 * by multiple threads are automatically reordered and do not need to be synchronized manually.
 * <li>{@link IPullEffect} - the {@link IPullEffect#process(ISingleChanAudioSource, float[])}
 * method must make exactly one call to {@link ISingleChanAudioSource#readSamples(float[])} to allow
 * proper parallelization. An {@link IllegalStateException} will be throws if more than one call,
 * or no call is done. Not that calls to {@link ISingleChanAudioSource#readSamples(float[])} from
 * different threads will be automatically serialized and do not need to be synchronized manually.
 * <li>All others interfaces: cannot be used
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiThreading {

    /**
     * Indicate that this plugin requires every audio chunk to be processed after the previous one.
     * <p>
     * This is used for instance by IIR filters, or by processings involving phase propagation.
     * <p>
     * The default value is <code>false</code>. When <code>true</code>, only multiple channels can be
     * run concurrently, but not audio chunks corresponding to different instants in time.
     * <p>
     * Whevever only parts of the processing needs to be executed in the correct order and in a sychronized
     * way, you way want to consider implementing {@link IHasSerialSections} instead and using
     * {@link ISerialSection} objects.
     */
    public boolean markov() default false;

}
