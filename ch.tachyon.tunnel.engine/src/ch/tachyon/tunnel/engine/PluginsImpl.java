package ch.tachyon.tunnel.engine;

import java.io.*;
import java.net.*;
import java.util.*;
import ch.tachyon.tunnel.common.utils.*;
import ch.tachyon.tunnel.engine.bridges.*;
import ch.tachyon.tunnel.engine.bridges.chan.expand.*;
import ch.tachyon.tunnel.engine.bridges.chan.reduce.*;
import ch.tachyon.tunnel.engine.bridges.mthread.*;
import ch.tachyon.tunnel.engine.utils.*;
import ch.tachyon.tunnel.host.IProcessingInfo;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.IPullEffect;
import ch.tachyon.tunnel.plugin.IPushEffect;
import ch.tachyon.tunnel.plugin.ISimpleEffect;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * Implementation of the internals of the {@link PluginBundleImpl} class.
 * <p>
 * <b>Multithreaded bridging notes:</b>
 * <p>
 * There exist a bridge between every combination of host and plugin interface that can
 * be bridged. In case both the host and the plugin support multi-threading, the following
 * scheme is used:
 * <ul>
 * <li>If the plugin is an {@link IPullEffect}, it is wrapped by {@link MtScPull} to make it
 * multi-threaded and then regular bridging into the host interface is done.
 * <li>If the plugin is an {@link IPushEffect}, its is wrapped by {@link MtScPush} to make it
 * multi-threaded and then regular bridging into the host interface is done
 * <li>If the plugin is an {@link ISimpleEffect},
 * <ul>
 * <li>If the host supports the {@link ISingleChanPushEffect}, the plugin is wrapped into an
 * {@link IPushEffect} using {@link BridgeScSimpleAsPush} and then wrapped by {@link MtScPush}
 * to make it multi-threaded. Then regular bridging into the host interface is done.
 * <li>If the host supports the {@link ISingleChanPullEffect}, the plugin is wrapped into an
 * {@link IPullEffect} using {@link BridgeScSimpleAsPull} and then wrapped by {@link MtScPull}
 * to make it multi-threaded. Then regular bridging into the host interface is done.
 * <li>Else (for instance if the host only supports the {@link ISingleChanEffect} interface),
 * multi-threading is not used.
 * </ul>
 * </ul>
 * Observe that multi-threading is implemented on the plugin interface, and not on the host interface.
 * <p>
 * <b>Single chan to multi-chan bridging notes:</b>
 * When the plugin interface is single chan, but the host interface is multi chan, conversion is done on the
 * host interface:
 * <ul>
 * <li>First a bridge is created, that implements the single chan version of the host interface
 * <li>Then the resulting bridge is wrapped by one of the converter: {@link McEffectFromScEffect}, {@link McPushFromScPush}, {@link McPullFromScPull}.
 * </ul>
 * Note that multithreading is always done on individual channels (each channel uses a separate thread). In addition,
 * Multithreading is also enabled on the plugin (as above), except if there are only two available cores.
 */
public class PluginsImpl {

    /**
     * Path (in the JAR file) of the resource that lists the plugin that are available in the bundle
     */
    private static final String PLUGINS_RESOURCE = "META-INF/services/ch.tachyon.tunnel.Plugins";

    private final static int PRIORITY_STRAIGHT = 0; // Same interface
    private final static int PRIORITY_EASY = 2; // Easy conversion
    private final static int PRIORITY_HARD = 4; // Hard conversion
//    private final static int PRIORITY_OPTIONAL = 6; // Hard conversion, only if requested by host


    /**
     * List of all plugin interface to host interface converters. Keys is plugin interface and host interface (as a pair),
     * value is the converter.
     * <p>
     * Single chan to multi chan conversion, and multi-threading wrappers are not included as they are done in separate steps
     */
    private final static Map<Pair<Class<? extends IPlugin>, Class<? extends IEffect>>, Class<? extends IEffect>> bridges =
        new HashMap<Pair<Class<? extends IPlugin>,Class<? extends IEffect>>, Class<? extends IEffect>>();
    
    /**
     * For each bridge in 'bridges', itw priority. Bridges of higher (smaller) priority are preferred. Higher priority
     * means less complexity in the conversion process
     */
    private final static Map<Class<? extends IEffect>, Integer> bridgePriorities = new HashMap<Class<? extends IEffect>, Integer>();
    
    /**
     * Single chan to multi chan converters. This is done after the plugin interface is bridged to the host interface.
     * Key is multi chan interface, value is single chan interface and converter (as a pair)
     */
    private final static Map<Class<? extends IEffect>, Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>>> singleToMultiConverters = new HashMap<>();
    
    /**
     * Multi chan to single chan converters. This is done after the plugin interface is bridged to the host interface.
     * Key is single chan interface, value is multi chan interface and converter (as a pair)
     */
    private final static Map<Class<? extends IEffect>, Pair<Class<? extends IEffect>, Class<? extends ScFromMcBase<?, ?>>>> multiToSingleConverters = new HashMap<>();
    
    /**
     * Set of host interfaces that correspond to multi chan interfaces
     */
    private final static Set<Class<? extends IEffect>> multiChanInterfaces = new HashSet<Class<? extends IEffect>>();

    /**
     * Initialize all the bridges. A bridge allows a plugin interface to be accessed using a host
     * interface. A bridge also attempts to
     * <ul>
     * <li>Automatically compensate for the plugin's latency
     * <li>Honor the plugin's desired fixed audio chunk size using rebuffering as necessary
     * <li>Parallelize the processing using a thread pool
     * <li>Convert from single chan to multi chan, using separate threads for each channel
     * </ul>
     */
    static {
        // Plugin interface --> Host interface
        // Sc Simple --> Sc Simple
        addBridge(ch.tachyon.tunnel.plugin.ISimpleEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanEffect.class,
                ScEffectFromScEffect.class, PRIORITY_STRAIGHT);
        // Mc Simple --> Mc Simple
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanEffect.class, ch.tachyon.tunnel.host.effect.ISimpleEffect.class,
                McEffectFromMcEffect.class, PRIORITY_STRAIGHT);
        // Sc Pull --> Sc Pull
        addBridge(ch.tachyon.tunnel.plugin.IPullEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPullEffect.class,
                ScPullFromScPull.class, PRIORITY_STRAIGHT);
        // Mc Pull --> Mc Pull
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanPullEffect.class, ch.tachyon.tunnel.host.effect.IPullEffect.class,
                McPullFromMcPull.class, PRIORITY_STRAIGHT);
        // Sc Push --> Sc Push
        addBridge(ch.tachyon.tunnel.plugin.IPushEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPushEffect.class,
                ScPushFromScPush.class, PRIORITY_STRAIGHT);
        // Mc Push --> Mc Push
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanPushEffect.class, ch.tachyon.tunnel.host.effect.IPushEffect.class,
                McPushFromMcPush.class, PRIORITY_STRAIGHT);
        // Sc Simple --> Sc Pull
        addBridge(ch.tachyon.tunnel.plugin.ISimpleEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPullEffect.class,
                ScPullFromScEffect.class, PRIORITY_EASY);
        // Mc Simple --> Mc Pull
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanEffect.class, ch.tachyon.tunnel.host.effect.IPullEffect.class,
                McPullFromMcEffect.class, PRIORITY_EASY);
        // Sc Simple --> Sc Push
        addBridge(ch.tachyon.tunnel.plugin.ISimpleEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPushEffect.class,
                ScPushFromScEffect.class, PRIORITY_EASY);
        // Mc Simple -> Mc Push
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanEffect.class, ch.tachyon.tunnel.host.effect.IPushEffect.class,
                McPushFromMcEffect.class, PRIORITY_EASY);
        // Sc Pull --> Sc Push
        addBridge(ch.tachyon.tunnel.plugin.IPullEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPushEffect.class,
                ScPushFromScPull.class, PRIORITY_HARD);
        // Mc Pull --> Mc Push
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanPullEffect.class, ch.tachyon.tunnel.host.effect.IPushEffect.class,
                McPushFromMcPull.class, PRIORITY_HARD);
        // Sc Push --> Sc Pull
        addBridge(ch.tachyon.tunnel.plugin.IPushEffect.class, ch.tachyon.tunnel.host.effect.ISingleChanPullEffect.class,
                ScPullFromScPush.class, PRIORITY_HARD);
        // Mc Push --> Mc Pull
        addBridge(ch.tachyon.tunnel.plugin.IMultiChanPushEffect.class, ch.tachyon.tunnel.host.effect.IPullEffect.class,
                McPullFromMcPush.class, PRIORITY_HARD);
        
        // Multi-chan interfaces
        multiChanInterfaces.add(ch.tachyon.tunnel.host.effect.ISimpleEffect.class);
        multiChanInterfaces.add(ch.tachyon.tunnel.host.effect.IPullEffect.class);
        multiChanInterfaces.add(ch.tachyon.tunnel.host.effect.IPushEffect.class);
        
        // Mono to stereo (multi-chan) converters
        addSingleToMultiConverter(ISingleChanEffect.class, ch.tachyon.tunnel.host.effect.ISimpleEffect.class, McEffectFromScEffect.class);
        addSingleToMultiConverter(ISingleChanPushEffect.class, ch.tachyon.tunnel.host.effect.IPushEffect.class, McPushFromScPush.class);
        addSingleToMultiConverter(ISingleChanPullEffect.class, ch.tachyon.tunnel.host.effect.IPullEffect.class, McPullFromScPull.class);
        
        // Stereo (multi-chan) to mono converters
        addMultiToSIngleConverter(ch.tachyon.tunnel.host.effect.ISimpleEffect.class, ISingleChanEffect.class, ScEffectFromMcEffect.class);
        addMultiToSIngleConverter(ch.tachyon.tunnel.host.effect.IPushEffect.class, ISingleChanPushEffect.class, ScPushFromMcPush.class);
        addMultiToSIngleConverter(ch.tachyon.tunnel.host.effect.IPullEffect.class, ISingleChanPullEffect.class, ScPullFromMcPull.class);
    }

    private static <P extends IPlugin, H extends IEffect> void addBridge(Class<P> pluginInterface, Class<H> hostInterface, Class<? extends H> bridgeType,
            int priority) {
        bridges.put(new Pair<Class<? extends IPlugin>, Class<? extends IEffect>>(pluginInterface, hostInterface), bridgeType);
        bridgePriorities.put(bridgeType, priority);
    }
    
    private static <S extends IEffect, M extends IEffect, C extends McFromScBase<M, S>> void addSingleToMultiConverter(Class<S> singleInterface, Class<M> multiInterface, Class<C> converter) {
        singleToMultiConverters.put(multiInterface, new Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>>(singleInterface, converter));
    }
    
    private static <M extends IEffect, S extends IEffect, C extends ScFromMcBase<S, M>> void addMultiToSIngleConverter(Class<M> multiInterface, Class<S> singleInterface, Class<C> converter) {
        multiToSingleConverters.put(singleInterface, new Pair<Class<? extends IEffect>, Class<? extends ScFromMcBase<?,?>>>(multiInterface, converter));
    }

    /**
     * Load all available plugins that can be bridged to at least one of the given host interfaces.
     * Plugins are searched in the classpath underlying the given {@link ClassLoader}. Only plugins
     * explicitely referenced in a <tt>META-INF/services/ch.tachyon.tunnel.Plugins</tt> file are considered.
     */
    public static List<IEffect> loadAllPlugins(ClassLoader classLoader, MtContext mtContext, List<Class<? extends IEffect>> hostInterfaces) throws IOException {
        List<IEffect> plugins = new ArrayList<IEffect>();
        Enumeration<URL> pluginLists = classLoader.getResources(PLUGINS_RESOURCE);
        while (pluginLists.hasMoreElements()) {
            URL pluginList = pluginLists.nextElement();
            InputStream stream = pluginList.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                String line = reader.readLine();
                while (line != null) {
                    if (!line.trim().equals("") && !line.trim().startsWith("#")) {
                        String className = line.trim();
                        IEffect effect = loadEffect(classLoader, hostInterfaces, className, mtContext);
                        if (effect != null)
                            plugins.add(effect);
                    }
                    line = reader.readLine();
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return plugins;
    }

    public static <E extends IEffect> E loadPlugin(ClassLoader classLoader, MtContext mtContext, Class<E> type, String className) {
        List<Class<? extends IEffect>> singleton = new ArrayList<Class<? extends IEffect>>(1);
        singleton.add(type);
        @SuppressWarnings("unchecked")
        E result = (E) loadEffect(classLoader, singleton, className, mtContext);
        assert result == null || type.isInstance(result);
        return result;
    }

    private static IEffect loadEffect(ClassLoader cl, List<Class<? extends IEffect>> hostInterfaces, String className, MtContext mtContext) {
        try {
            // Load the plugin class
            Class<?> type = Class.forName(className, true, cl);
            if (!IPlugin.class.isAssignableFrom(type)) {
                Debug.error("Not a plugin: {0}", className);
                return null;
            }
            // Get implemented plugin interface
            @SuppressWarnings("unchecked")
            Class<? extends IPlugin> effectType = (Class<? extends IPlugin>) type;
            return createEffect(hostInterfaces, effectType, mtContext);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Create an effect of the given plugin type and wrap it to accept one of the given host interfaces.
     * Returns <code>null</code> if none of the plugin interfaces can be bridged to any of the host interfaces.
     */
    private static IEffect createEffect(List<Class<? extends IEffect>> hostInterfaces, Class<? extends IPlugin> effectType, MtContext mtContext) throws InstantiationException,
            IllegalAccessException {
        List<Class<? extends IPlugin>> effectInterfaces = getEffectInterfaces(effectType);
        for (Class<? extends IPlugin> effectInterface : effectInterfaces) {
            IEffect result = createEffect(hostInterfaces, effectType, effectInterface, mtContext);
            if (result != null)
                return result;
        }
        return null;
    }

    private static Properties loadProperties(Class<?> forClass) {
        Properties properties = new Properties();
        InputStream input = forClass.getResourceAsStream(forClass.getSimpleName() + ".properties");
        if (input == null)
            return properties;
        try {
            // Convert the input stream from UTF-8 to ISO-8859-1 to satisfy Properties#load(InputStream)
            CodecInputStream codec = new CodecInputStream(input, "UTF-8", "ISO-8859-1");
            properties.load(codec);
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }
    
    static class ConversionInfo {
        Class<? extends IPlugin> pluginInterface;
        Class<? extends IEffect> targetInterface;
        Class<? extends McFromScBase<?, ?>> chanExpanderType;
        Class<? extends ScFromMcBase<?, ?>> chanReducerType;
        boolean isWorthMultiThreading;
        
        public ConversionInfo(Class<? extends IPlugin> pluginInterface, Class<? extends IEffect> targetInterface,
                Class<? extends McFromScBase<?, ?>> expanderType, Class<? extends ScFromMcBase<?, ?>> reducerType, boolean isWorthMultiThreading) {
            this.pluginInterface = pluginInterface;
            this.targetInterface = targetInterface;
            this.chanExpanderType = expanderType;
            this.chanReducerType = reducerType;
            this.isWorthMultiThreading = isWorthMultiThreading;
        }
    }
    
    /**
     * Create an effect of the given plugin type using the given plugin interface, and wrap it to accept one of the given host interfaces.
     * Returns <code>null</code> if the plugin interface cannot be bridged to any of the host interfaces.
     */
    private static IEffect createEffect(List<Class<? extends IEffect>> hostInterfaces, final Class<? extends IPlugin> effectType,
            Class<? extends IPlugin> effectInterface, MtContext mtContext)
                    throws InstantiationException, IllegalAccessException {
        // Create extended list of host interfaces, 
        // adding single chan to multi chan conversion
        List<Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>>> targetMcInterfaces = new ArrayList<>();
        for (Class<? extends IEffect> hostInterface : hostInterfaces) {
            targetMcInterfaces.add(new Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?,?>>>(hostInterface, null)); // No conversion
            if (singleToMultiConverters.containsKey(hostInterface)) { // Multi chan can be converted
                Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>> pair = singleToMultiConverters.get(hostInterface);
                targetMcInterfaces.add(pair); // Add corresponding single chan, and converter type
            }
        }
        // and multi chan to single chan conversion
        List<Pair<Class<? extends IEffect>, Class<? extends ScFromMcBase<?, ?>>>> targetScInterfaces = new ArrayList<>();
        for (Class<? extends IEffect> hostInterface : hostInterfaces) {
            if (multiToSingleConverters.containsKey(hostInterface)) { // Single chan can be converted
                Pair<Class<? extends IEffect>, Class<? extends ScFromMcBase<?, ?>>> pair = multiToSingleConverters.get(hostInterface);
                targetScInterfaces.add(pair); // Add corresponding multi chan, and converter type
            }
        }
        
        // Load associated properties
        Properties properties = loadProperties(effectType);
        
        // Check if multithreadable
        Class<? extends IPlugin> mtEffectInterface = null;
        MultiThreading mtAnno = AnnotationUtils.getAnnotation(effectType, MultiThreading.class, properties);
        if (isMultiThreadingSupported(mtAnno, mtContext)) {
            if (effectInterface.equals(ISimpleEffect.class)) {
                /*
                 * Attempt to multithread an ISimpleEffect, by converting it to a IPushEffect or IPUllEffect.
                 * However, we must check that the host supports one of those interfaces
                 */
                boolean hostPushOk = false;
                boolean hostPullOk = false;
                for (Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>> pair : targetMcInterfaces) {
                    Class<?> hostInterface = pair.getFirst();
                    if (hostInterface.equals(ch.tachyon.tunnel.host.effect.ISingleChanPushEffect.class))
                        hostPushOk = true;
                    if (hostInterface.equals(ch.tachyon.tunnel.host.effect.ISingleChanPullEffect.class))
                        hostPullOk = true;
                }
                // Ok, multithreading can be done by converting the plugin to an IPushEffect or IPullEffect
                if (hostPushOk) // We slightly prefer the PUSH interface over the PULL one
                    mtEffectInterface = IPushEffect.class;
                else if (hostPullOk)
                    mtEffectInterface = IPullEffect.class;
            }
            if (effectInterface.equals(IMultiChanEffect.class)) {
                /*
                 * Attempt to multithread an IMultiChanEffect, by converting it to a IMultiChanPushEffect or IMultiChanPUllEffect.
                 * However, we must check that the host supports one of those interfaces
                 */
                boolean hostPushOk = false;
                boolean hostPullOk = false;
                for (Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>> pair : targetMcInterfaces) {
                    Class<?> hostInterface = pair.getFirst();
                    if (hostInterface.equals(ch.tachyon.tunnel.host.effect.IPushEffect.class))
                        hostPushOk = true;
                    if (hostInterface.equals(ch.tachyon.tunnel.host.effect.IPullEffect.class))
                        hostPullOk = true;
                }
                // Ok, multithreading can be done by converting the plugin to an IPushEffect or IPullEffect
                if (hostPushOk) // We slightly prefer the PUSH interface over the PULL one
                    mtEffectInterface = IMultiChanPushEffect.class;
                else if (hostPullOk)
                    mtEffectInterface = IMultiChanPullEffect.class;
            }
        }
        boolean isMultiThreadable = (mtEffectInterface != null);
        
        // Look for a suitable converter to one of the specified host interfaces
        // If multiple converters exist, use the one of highest priority
        Pair<Class<? extends IPlugin>,Class<? extends IEffect>> bestConversion = null;
        ConversionInfo bestInfo = null;
        int bestPriority = Integer.MAX_VALUE;
        for (Pair<Class<? extends IEffect>, Class<? extends McFromScBase<?, ?>>> pair : targetMcInterfaces) {
            Class<? extends IEffect> hostInterface = pair.getFirst();
            boolean isParallelizingChans = (pair.getSecond() != null);
            boolean isWorthMultithreading = !isParallelizingChans || mtContext.getNbThreads() > 2;
            Class<? extends IPlugin> pluginInterface = (isWorthMultithreading && isMultiThreadable ? mtEffectInterface : effectInterface);
            ConversionInfo conversionInfo = new ConversionInfo(pluginInterface, hostInterface, pair.getSecond(), null, isWorthMultithreading);
            Pair<Class<? extends IPlugin>,Class<? extends IEffect>> conversion = new Pair<Class<? extends IPlugin>, Class<? extends IEffect>>(pluginInterface, hostInterface);
            if (bridges.containsKey(conversion)) {
                // This effect can be bridged to the target type
                @SuppressWarnings("unchecked")
                Class<IEffect> converterType = (Class<IEffect>) bridges.get(conversion);
                int priority = bridgePriorities.get(converterType);
                // Check if the conversion is better than what we currently have
                if (priority < bestPriority) {
                    bestConversion = conversion;
                    bestInfo = conversionInfo;
                    bestPriority = priority;
                }
            }
        }
        // Check for multi chan to single chan conversion
        boolean monoOk = true;
        Channels channelsAnno = AnnotationUtils.getAnnotation(effectType, Channels.class, properties);
        if (channelsAnno != null && channelsAnno.min() > 1)
            monoOk = false; // This effect does not work with a single channel
        if (monoOk) {
            for (Pair<Class<? extends IEffect>, Class<? extends ScFromMcBase<?, ?>>> pair : targetScInterfaces) {
                Class<? extends IEffect> hostInterface = pair.getFirst();
                ConversionInfo conversionInfo = new ConversionInfo(effectInterface, hostInterface, null, pair.getSecond(), false);
                Pair<Class<? extends IPlugin>,Class<? extends IEffect>> conversion = new Pair<Class<? extends IPlugin>, Class<? extends IEffect>>(effectInterface, hostInterface);
                if (bridges.containsKey(conversion)) {
                    // This effect can be bridged to the target type
                    @SuppressWarnings("unchecked")
                    Class<IEffect> converterType = (Class<IEffect>) bridges.get(conversion);
                    int priority = bridgePriorities.get(converterType) + 1; // Add 1 because using stereo interface for mono processing is not recommanded
                    // Check if the conversion is better than what we currently have
                    if (priority < bestPriority) {
                        bestConversion = conversion;
                        bestInfo = conversionInfo;
                        bestPriority = priority;
                    }
                }
            }
        }
        if (bestConversion == null)
            return null; // No converter found. This plugin cannot be bridged to any of the host interface and hence cannot be loaded
        
        // Create the plugin
        IPlugin plugin = effectType.newInstance();
        
        // Create the converter (bridge)
        @SuppressWarnings("unchecked")
        final Class<IEffect> converterType = (Class<IEffect>) bridges.get(bestConversion);
        final boolean isMultiThreadingWorth = bestInfo.isWorthMultiThreading;
//        Debug.info("Bridging {0} using {1}", effectType.getSimpleName(), converterType.getSimpleName());
        final IEffect bridged = newConverter(converterType, bestInfo.pluginInterface, plugin, properties, mtAnno, mtContext, isMultiThreadingWorth);
        IEffect converted = bridged;
        
        // Convert channels if necessary
        if (bestInfo.chanExpanderType != null) {
            @SuppressWarnings("unchecked")
            McFromScBase<IEffect, IEffect> chanExpander = (McFromScBase<IEffect, IEffect>) bestInfo.chanExpanderType.newInstance();
            final Class<? extends IPlugin> effectInterface0 = bestInfo.pluginInterface;
            final Properties properties0 = properties;
            final MultiThreading mtAnno0 = mtAnno;
            final MtContext mtContext0 = mtContext;
            chanExpander.init(bridged, new IScEffectFactory<IEffect>() {
                
                public IEffect create(IEffect mainEffect, IProcessingInfo info) {
                    try {
                        IPlugin plugin = effectType.newInstance();
                        return newConverter(converterType, effectInterface0, plugin, properties0, mtAnno0, mtContext0, isMultiThreadingWorth);
                    } catch (IllegalAccessException ex) {
                        // This should not happen as we could already create a new instance before
                        throw new RuntimeException(ex);
                    } catch (InstantiationException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            converted = chanExpander;
        } else if (bestInfo.chanReducerType != null) {
            @SuppressWarnings("unchecked")
            ScFromMcBase<IEffect, IEffect> chanReducer = (ScFromMcBase<IEffect, IEffect>) bestInfo.chanReducerType.newInstance();
            chanReducer.init(bridged);
            converted = chanReducer;
        }
        // Return the converted effect
        return converted;
    }

    /**
     * Get the first {@link IPlugin} interface implemented by the given plugin type
     */
    private static List<Class<? extends IPlugin>> getEffectInterfaces(Class<? extends IPlugin> effectType) {
        List<Class<? extends IPlugin>> result = new ArrayList<Class<? extends IPlugin>>();
        Class<?> currentType = effectType;
        do {
            Class<?>[] interfaces = currentType.getInterfaces();
            for (Class<?> intf : interfaces) {
                if (interfaceExtends(intf, IPlugin.class)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IPlugin> pluginIntf = (Class<? extends IPlugin>) intf;
                    result.add(pluginIntf);
                }
            }
            currentType = currentType.getSuperclass();
        } while (currentType != null);
        return result;
    }

    private static boolean interfaceExtends(Class<?> intf, Class<?> parent) {
        Class<?>[] parents = intf.getInterfaces();
        for (Class<?> p : parents)
            if (p.equals(parent))
                return true;
        return false;
    }

    /**
     * Bridge the given plugin from the plugin interface 'targetType' to the host interface 'converterType'
     */
    private static <H extends IEffect, P extends IPlugin> H newConverter(Class<H> converterType, Class<P> targetType, IPlugin target, Properties properties, MultiThreading mtSpecs, MtContext mtContext,
            boolean isMultiThreadingWorth) {
        IPlugin processingTarget = target;
        try {
            if (isMultiThreadingWorth && isMultiThreadingSupported(mtSpecs, mtContext)) {
                if (processingTarget instanceof ISimpleEffect && targetType.equals(IPushEffect.class))
                    // Wrap to an IPushEffect to allow multithreading
                    processingTarget = new BridgeScSimpleAsPush((ISimpleEffect) processingTarget);
                if (processingTarget instanceof ISimpleEffect && targetType.equals(IPullEffect.class))
                    // Wrap to an IPullEffect to allow multithreading
                    processingTarget = new BridgeScSimpleAsPull((ISimpleEffect) processingTarget);
                if (processingTarget instanceof IMultiChanEffect && targetType.equals(IMultiChanPushEffect.class))
                    // Wrap to an IMultiChanPushEffect to allow multithreading
                    processingTarget = new BridgeMcSimpleAsPush((IMultiChanEffect) processingTarget);
                if (processingTarget instanceof IMultiChanEffect && targetType.equals(IMultiChanPullEffect.class))
                    // Wrap to an IMultiChanPullEffect to allow multithreading
                    processingTarget = new BridgeMcSimpleAsPull((IMultiChanEffect) processingTarget);

                // Wrap into a multithreaded version if available
                if (processingTarget instanceof IPushEffect)
                    processingTarget = new MtScPush((IPushEffect) processingTarget, mtContext);
                else if (processingTarget instanceof IPullEffect)
                    processingTarget = new MtScPull((IPullEffect) processingTarget, mtContext);
                else if (processingTarget instanceof IMultiChanPushEffect)
                    processingTarget = new MtMcPush((IMultiChanPushEffect) processingTarget, mtContext);
                else if (processingTarget instanceof IMultiChanPullEffect)
                    processingTarget = new MtMcPull((IMultiChanPullEffect) processingTarget, mtContext);
            }

            // Create the bridge
            H result = converterType.newInstance();
            PluginToHostWrapper<?> wrapper = (PluginToHostWrapper<?>) result;
            wrapper.init(target, processingTarget, mtContext, properties);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static boolean isMultiThreadingSupported(MultiThreading mtAnno, MtContext mtContext) {
        // Check if plugin can do multi-threading
        boolean result = (mtAnno != null && !mtAnno.markov());
        // Check if architecture supports it
        if (!mtContext.isMultiCore())
            result = false; // Some multi-threaded classes (such as MtScPush) still works, but this brings useless overhead. Other classes do not work at all (MtScPull)
        return result;
    }

    /**
     * Load a plugin statically. Unlike {@link #loadPlugin(ClassLoader, MtContext, Class, String)} that is used to load
     * plugins dynamically from external .jar files, this method is used to load a plugin that is known
     * and that is already in the current class path.
     * @param <I> the interface you want to use to communicate with the plugin
     * @param <E> the plugin class
     * @param commIntf the interface you want to use to communicate with the plugin
     * @param pluginClass the plugin class
     * @return the requested plugin, wrapped in the requested interface
     */
    public static <I extends IEffect, E extends IPlugin> I getPlugin(Class<I> commIntf, Class<E> pluginClass, MtContext mtContext) {
        List<Class<? extends IEffect>> singleton = new ArrayList<Class<? extends IEffect>>(1);
        singleton.add(commIntf);
        try {
            @SuppressWarnings("unchecked")
            I result = (I) createEffect(singleton, pluginClass, mtContext);
            return result;
        } catch (IllegalAccessException ex) {
            throw new IllegalAccessError(ex.toString());
        } catch (InstantiationException ex) {
            throw new InstantiationError(ex.toString());
        }
    }

}
