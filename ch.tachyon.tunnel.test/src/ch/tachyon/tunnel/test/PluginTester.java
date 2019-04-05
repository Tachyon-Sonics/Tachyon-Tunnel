package ch.tachyon.tunnel.test;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import junit.extensions.*;
import junit.framework.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;

/**
 * TestCase for a single plugin.
 * <p>
 * Override and use {@link #PluginTester(IPluginBundle, String)} with your bundle abd the name of the effect to
 * test.
 * <p>
 * To test all effects in a bundle, use {@link PluginBundleTester}.
 */
public class PluginTester {

    private final static float MIN_RATIO_TIME_CHANGE = 0.99f;
    private final static float MAX_RATIO_TIME_CHANGE = 1.01f;
    private final static float MAX_FRAMES_TIME_CHANGE = 10.0f;

    private final Random rnd = new Random();
    
    private List<WeakReference<Class<?>>> classRefs; // TODO adapt
    private IPluginBundle bundle;
    private String effectName;

    
    
    public PluginTester(IPluginBundle bundle, String effectName) {
        this(null, bundle, effectName);
    }

    public PluginTester(List<WeakReference<Class<?>>> classRefs, IPluginBundle bundle, String effectName) {
//        super(effectName);
        this.classRefs = classRefs;
        this.bundle = bundle;
        this.effectName = effectName;
    }
    
    public TestSuite suite() {
        TestSuite suite = new TestSuite(effectName);
        {
            TestSuite serialSuite = new ActiveTestSuite("Single threaded");
            suite.addTest(serialSuite);
            {
                TestSuite scSuite = new TestSuite("Single channel");
                serialSuite.addTest(scSuite);
                LazyScPlugin plugin = new LazyScPlugin(bundle, effectName, false);
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, true, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10, 10, true, 1000));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10000, 10000, true, 3));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10, 5000, true, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, false, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, true, 8));
            }
            for (int chans = 1; chans <= 3; chans++) {
                TestSuite mcSuite = new TestSuite("" + chans + " channels");
                serialSuite.addTest(mcSuite);
                LazyMcPlugin plugin = new LazyMcPlugin(bundle, effectName, chans, false);
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 10, true, 1000));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10000, 10000, true, 3));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 5000, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, false, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 8));
            }
        }
        {
            TestSuite parallelSuite = new ActiveTestSuite("Multi threaded");
            suite.addTest(parallelSuite);
            {
                TestSuite scSuite = new TestSuite("Single channel");
                parallelSuite.addTest(scSuite);
                LazyScPlugin plugin = new LazyScPlugin(bundle, effectName, true);
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, true, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10, 10, true, 1000));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10000, 10000, true, 3));
                scSuite.addTest(new PluginScProcessingTest(plugin, 10, 5000, true, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, false, 10));
                scSuite.addTest(new PluginScProcessingTest(plugin, 2048, 2048, true, 8));
            }
            for (int chans = 1; chans <= 3; chans++) {
                TestSuite mcSuite = new TestSuite("" + chans + "channels");
                parallelSuite.addTest(mcSuite);
                LazyMcPlugin plugin = new LazyMcPlugin(bundle, effectName, chans, true);
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 10, true, 1000));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10000, 10000, true, 3));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 5000, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, false, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 8));
            }
        }
        return suite;
    }

//    @Override
    protected void runTest() throws ClassNotFoundException {
        { // Single threaded
            bundle.getMtContext().setNbThreads(1);
            ISingleChanPushEffect plugin = bundle.loadPlugin(ISingleChanPushEffect.class, effectName);
            if (plugin != null) {
                // Single chan
                Class<?> pluginClass = Class.forName(plugin.getClassName(), true, bundle.getClassLoader(false));
                if (classRefs != null)
                    classRefs.add(new WeakReference<Class<?>>(pluginClass));
   
                System.out.println("[Process, size=2048, mono] " + effectName);
                runEffect(plugin, 2048, 2048, true, 10);
                System.out.println("[Process, size=10, mono] " + effectName);
                runEffect(plugin, 10, 10, true, 1000);
                System.out.println("[Process, size=10000, mono] " + effectName);
                runEffect(plugin, 10000, 10000, true, 3);
                System.out.println("[Process, size=varying, mono] " + effectName);
                runEffect(plugin, 10, 5000, true, 10);
                System.out.println("[Process & abort, mono] " + effectName);
                runEffect(plugin, 2048, 2048, false, 10);
                System.out.println("[Process after abort, size=2048, mono] " + effectName);
                runEffect(plugin, 2048, 2048, true, 10);
            } 
            {
                // Multi chan
                IPushEffect mcPlugin = bundle.loadPlugin(IPushEffect.class, effectName);
                if (mcPlugin != null) {
                    Class<?> pluginClass = Class.forName(mcPlugin.getClassName(), true, bundle.getClassLoader(false));
                    if (classRefs != null)
                        classRefs.add(new WeakReference<Class<?>>(pluginClass));
                    
                    int minNbChan = 1;
                    int maxNbChan = 3;
                    Channels channels = mcPlugin.getAnnotation(Channels.class);
                    if (channels != null) {
                        minNbChan = Math.max(minNbChan, channels.min());
                        maxNbChan = Math.min(maxNbChan, channels.max());
                    }
                    
                    for (int nbChan = minNbChan; nbChan <= maxNbChan; nbChan++) {
                        System.out.println("[Process, size=2048, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 2048, 2048, true, 10, nbChan);
                        System.out.println("[Process, size=10, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10, 10, true, 1000, nbChan);
                        System.out.println("[Process, size=10000, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10000, 10000, true, 3, nbChan);
                        System.out.println("[Process, size=varying, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10, 5000, true, 10, nbChan);
                        System.out.println("[Process & abort, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 2048, 2048, false, 10, nbChan);
                    }
                } else
                    System.out.println("Could not test " + effectName + " as IPushEffect (multi chan, single thread)");
            }
        }

        { // Multithreaded
            bundle.getMtContext().setNbThreads(Math.max(4, Runtime.getRuntime().availableProcessors()));
            ISingleChanPushEffect plugin = bundle.loadPlugin(ISingleChanPushEffect.class, effectName);
            if (plugin != null) {
                // Single chan
                Class<?> pluginClass = Class.forName(plugin.getClassName(), true, bundle.getClassLoader(false));
                if (classRefs != null)
                    classRefs.add(new WeakReference<Class<?>>(pluginClass));
   
                System.out.println("[MT Process, size=2048, mono] " + effectName);
                runEffect(plugin, 2048, 2048, true, 10);
                System.out.println("[MT Process, size=10, mono] " + effectName);
                runEffect(plugin, 10, 10, true, 1000);
                System.out.println("[MT Process, size=10000, mono] " + effectName);
                runEffect(plugin, 10000, 10000, true, 3);
                System.out.println("[MT Process, size=varying, mono] " + effectName);
                runEffect(plugin, 10, 5000, true, 10);
                System.out.println("[MT Process & abort, mono] " + effectName);
                runEffect(plugin, 2048, 2048, false, 10);
            } 
            {
                // Multi chan
                IPushEffect mcPlugin = bundle.loadPlugin(IPushEffect.class, effectName);
                if (mcPlugin != null) {
                    Class<?> pluginClass = Class.forName(mcPlugin.getClassName(), true, bundle.getClassLoader(false));
                    if (classRefs != null)
                        classRefs.add(new WeakReference<Class<?>>(pluginClass));
                    
                    int minNbChan = 1;
                    int maxNbChan = 3;
                    Channels channels = mcPlugin.getAnnotation(Channels.class);
                    if (channels != null) {
                        minNbChan = Math.max(minNbChan, channels.min());
                        maxNbChan = Math.min(maxNbChan, channels.max());
                    }
                    
                    for (int nbChan = minNbChan; nbChan <= maxNbChan; nbChan++) {
                        System.out.println("[MT Process, size=2048, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 2048, 2048, true, 10, nbChan);
                        System.out.println("[MT Process, size=10, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10, 10, true, 1000, nbChan);
                        System.out.println("[MT Process, size=10000, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10000, 10000, true, 3, nbChan);
                        System.out.println("[MT Process, size=varying, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 10, 5000, true, 10, nbChan);
                        System.out.println("[MT Process & abort, nbChans=" + nbChan + "] " + effectName);
                        runMcEffect(mcPlugin, 2048, 2048, false, 10, nbChan);
                    }
                } else
                    System.out.println("Could not test " + effectName + " as IPushEffect (multichan, multithread)");
            }
        }
    }

    private void runEffect(ISingleChanPushEffect plugin, int minSize, int maxSize, boolean cleanup, int count) {
        ProcessingInfo info = new ProcessingInfo(1, 44100.0f, 16, -1, maxSize, (minSize == maxSize ? maxSize : -1));
        plugin.load(info);
        plugin.startProcessing(info);
        float timeChange = plugin.getTimeChange();
        int nbInput = 0;
        final AtomicLong nbOutput = new AtomicLong();
        for (int k = 0; k < count; k++) {
            int size = (minSize == maxSize ? maxSize : minSize + rnd.nextInt(maxSize - minSize + 1));
            nbInput += size;
            plugin.process(new float[size], size, new ISingleChanAudioSink() {

                @Override
                public void writeSamples(float[] samples) {
                    nbOutput.addAndGet(samples.length);
                }

                @Override
                public void close() {
                }
            });
        }
        if (cleanup) {
            nbInput += minSize / 2;
            plugin.process(new float[minSize], minSize / 2, new ISingleChanAudioSink() {

                @Override
                public void writeSamples(float[] samples) {
                    nbOutput.addAndGet(samples.length);
                }

                @Override
                public void close() {
                }
            });
        }
        plugin.stopProcessing();
        try {
            if (cleanup) {
                if (timeChange == 1.0f)
                    assert nbInput == nbOutput.get() : "The amount of produced frames (" + nbOutput.get() + ") does not match the amount of input frames (" + nbInput + ")";
                else if (timeChange > 0.0f) {
                    float expected = nbInput * timeChange;
                    float ratio = expected / (float) nbOutput.get();
                    assert (ratio >= MIN_RATIO_TIME_CHANGE && ratio <= MAX_RATIO_TIME_CHANGE) || (Math.abs(expected - nbOutput.get()) <= MAX_FRAMES_TIME_CHANGE) :
                        "The amount of produced samples (" + nbOutput.get() + ") differs significantly from the expected amount of "
                        + nbInput + " * " + plugin.getTimeChange() + " (plugin.getTimeChange()) = " + expected;
                }
            }
        } finally {
            plugin.unload();
        }
    }
    
    private void runMcEffect(IPushEffect plugin, int minSize, int maxSize, boolean cleanup, int count, int nbChannels) {
        ProcessingInfo info = new ProcessingInfo(nbChannels, 44100.0f, 16, -1, maxSize, (minSize == maxSize ? maxSize : -1));
        plugin.load(info);
        plugin.startProcessing(info);
        float timeChange = plugin.getTimeChange();
        int nbInput = 0;
        final AtomicLong nbOutput = new AtomicLong();
        for (int k = 0; k < count; k++) {
            int size = (minSize == maxSize ? maxSize : minSize + rnd.nextInt(maxSize - minSize + 1));
            nbInput += size;
            plugin.process(new float[nbChannels][size], size, new IMultiChanAudioSink() {

                @Override
                public void writeSamples(float[][] samples) {
                    int count = samples[0].length;
                    for (int k = 1; k < samples.length; k++)
                        assert samples[k].length == count : "Not all channels have the same sample count";
                    nbOutput.addAndGet(count);
                }

                @Override
                public void close() {
                }
            });
        }
        if (cleanup) {
            nbInput += minSize / 2;
            plugin.process(new float[nbChannels][minSize], minSize / 2, new IMultiChanAudioSink() {

                @Override
                public void writeSamples(float[][] samples) {
                    int count = samples[0].length;
                    for (int k = 1; k < samples.length; k++)
                        assert samples[k].length == count : "Not all channels have the same sample count";
                    nbOutput.addAndGet(count);
                }

                @Override
                public void close() {
                }
            });
        }
        plugin.stopProcessing();
        try {
            if (cleanup) {
                if (timeChange == 1.0f)
                    assert nbInput == nbOutput.get() : "The amount of produced frames (" + nbOutput.get() + ") does not match the amount of input frames (" + nbInput + ")";
                else if (timeChange > 0.0f) {
                    float expected = nbInput * timeChange;
                    float ratio = expected / (float) nbOutput.get();
                    assert (ratio >= MIN_RATIO_TIME_CHANGE && ratio <= MAX_RATIO_TIME_CHANGE) || (Math.abs(expected - nbOutput.get()) <= MAX_FRAMES_TIME_CHANGE) :
                        "The amount of produced frames (" + nbOutput.get() + ") differs significantly from the expected amount of "
                        + nbInput + " * " + plugin.getTimeChange() + " (plugin.getTimeChange()) = " + expected;
                }
            }
        } finally {
            plugin.unload();
        }
    }


}
