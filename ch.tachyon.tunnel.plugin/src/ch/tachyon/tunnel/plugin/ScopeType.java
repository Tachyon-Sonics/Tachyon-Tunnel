package ch.tachyon.tunnel.plugin;

import java.io.*;
import java.util.prefs.*;

import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;


/**
 * Scope type for the visibility and life time of a plugin's shared data.
 * This enumeration is used when a plugin needs to load and store shared data
 * using the {@link IStorage} interface.
 * @see #CHANNEL_PROCESS
 * @see #AUDIO_PROCESS
 * @see #PLUGIN_SESSION
 * @see #HOST_SESSION
 * @see #USER_PERSISTENT
 */
public enum ScopeType {
    /**
     * The data is maintained from the begin to the end of the processing (see {@link IStartStop}).
     * The data is common to all instances processing the same audio channel (when parallel processing
     * is performed).
     * <p>
     * This scope matches the scope of data stored in a {@link ISerialSection}.
     */
    CHANNEL_PROCESS,
    /**
     * This scope is similar to {@link #CHANNEL_PROCESS}. However, if multiple instances of the plugin are
     * used to process multiple audio <i>channels</i> with the same parameters in parallel, this scope is shared among all instances
     * used for all channels. On the other hand, {@link #CHANNEL_PROCESS} uses independent storage for each channel.
     * <p>
     * If multiple channels are processed one after the other and not in parallel, this scope is identical to
     * {@link #CHANNEL_PROCESS}.
     * <p>
     * This scope can be used to create and store read-only data that depends on the parameters and
     * that is necessary for processing.
     * <p>
     * In general this is done by implementing the {@link IStartStop#startProcessing(IProcessingInfo)} method as
     * <pre>
     * IStorage storage = {@link IProcessingInfo#getStorage() info.getStorage()};
     * MyData commonData = {@link IStorage#load(String, ScopeType, Class) storage.load("mykey", ScopeType.AUDIO_PROCESS, MyData.class)};
     * if (commonData == null) {
     *     commonData = computeCommonData();
     *     {@link IStorage#store(String, ScopeType, Object) storage.store("mykey", ScopeType.AUDIO_PROCESS, commonData)};
     * }
     * </pre>
     * Using this scheme ensures that the common data is computed only once, even when running on a multi-core CPU.
     * <p>
     * The above can be simplified to
     * <pre>
     * IStorage storage = {@link IProcessingInfo#getStorage() info.getStorage()};
     * MyData commonData = {@link IStorage#getProcessData(Class) storage.getProcessData(MyData.class)};
     * if (commonData == null) {
     *     commonData = computeCommonData();
     *     {@link IStorage#setProcessData(Object) storage.setProcessData(commonData)};
     * }
     * </pre>
     * @see MultiThreading
     */
    AUDIO_PROCESS,
    /**
     * The data is maintained from the time the plugin is loaded to the time it is unloaded.
     * The data is common to all additionnal instances created for parallel processing.
     * <p>
     * This scope can be used to create and store read-only data that is necessary for processing but does
     * not depend on the parameters.
     * <p>
     * This scope is preferred over {@link #HOST_SESSION} as it only uses memory while the plugin is loaded.
     */
    PLUGIN_SESSION,
    /**
     * The data is maintained during the whole time the host is running.
     * <p>
     * It can be used to store analyzed data in a two-step process. For example to store an analyzed
     * noise profile in a noise reduction plugin.
     * <p>
     * A plugin should not try to store the last values of its parameters, this is automatically done
     * by the engine.
     * <p>
     * <b>Warning</b>: data stored in this scope will eat memory for the whole life time of the host.
     * If you have a large amount of data to store, it is recommended to store it in a temporary
     * file instead:
     * <ul>
     * <li>Create a temporary file using {@link File#createTempFile(String, String)}.
     * <li>Store your data in that file
     * <li><i>Do not forget</i> to call {@link File#deleteOnExit()} on the created file
     * <li>Only store the corresponding {@link File} object in this scope
     * </ul>
     */
    HOST_SESSION,
    /**
     * The data is stored on the harddisk so that it persists for ever, even when the host is quit and restarted.
     * The data is stored separately for each user (where 'user' denotes the user logged in the operating system).
     * <p>
     * Do not use this scope to try to manually store presets; this is already handled by the engine.
     * <p>
     * This scope can be used to store registration serial keys, or flags such as "do not show again"
     * for a warning  or hint message.
     * <p>
     * <b>Warning:</b> this scope is very limited in capacity. It is backed by {@link Preferences}
     * and has the same limitations. Data is limited to a few primitive types, <code>String</code>
     * and <code>byte[]</code>, with limited length. If you need to store larger amount of data:
     * <ul>
     * <li>Create a folder in {@link System#getProperty(String) System.getProperty("user.home")}
     * such as <code>.<i>myplugin</i></code> and create a file in that folder
     * <li>Put your data in the created file
     * <li>If necessary, store the <i>path</i> to that file (and not the actual data) using this scope
     * </ul>
     * For a plugin <code>MyPlugin</code>, this scope uses {@link Preferences#userNodeForPackage(Class)
     * Preferences.userNodeForPackage(MyPlugin.class)}.
     */
    USER_PERSISTENT,
    /**
     * The data is stored on the harddisk so that it persists for ever. The data is stored
     * system-wide and is common to all users.
     * <p>
     * Do not use this scope for resources used by the plugin (images, curves, impulse responses, etc). The
     * proper way of packaging such resources is to put them in the .jar file and to use {@link Class#getResourceAsStream(String)}.
     * If native libraries are required, they must also be stored in the .jar file, and must be extracted to a temporary
     * file on the fly whenever they need to be loaded.
     * <p>
     * <b>Warning:</b> This scope should be avoided if possible because it might not be writable, for instance by a non-administrator
     * user. If the scope is not writable, {@link IStorage#store(String, ScopeType, Object)} will throw a {@link SecurityException}.
     * <p>
     * <b>Warning:</b> this scope is very limited in capacity. It is backed by {@link Preferences}
     * and has the same limitations. Data is limited to a few primitive types, <code>String</code>
     * and <code>byte[]</code>, with limited length. If you need to store larger amount of data the recommended way is to:
     * <ul>
     * <li>Create the folder <code>{@link System#getProperty(String) System.getProperty("user.dir")} + {@link File#separator} + ".<i>mypackage</i>_data"</code>
     * if it does not exist, and create a file in that folder
     * <li>Put your data in the created file
     * <li>If necessary, store the <i>path</i> to that file (and not the actual data) using this scope
     * </ul>
     * For a plugin <code>MyPlugin</code>, this scope uses {@link Preferences#systemNodeForPackage(Class)
     * Preferences.systemNodeForPackage(MyPlugin.class)}.
     */
    SYSTEM_PERSISTENT;
}
