package ch.tachyon.tunnel.test;

import java.lang.ref.*;
import java.util.*;
import ch.tachyon.tunnel.host.engine.*;
import junit.extensions.*;
import junit.framework.*;

/**
 * TestCase for a single plugin.
 * <p>
 * Override and use {@link #PluginTester(IPluginBundle, String)} with your bundle abd the name of the effect to
 * test.
 * <p>
 * To test all effects in a bundle, use {@link PluginBundleTester}.
 */
public class PluginTester {

    private List<WeakReference<Class<?>>> classRefs;
    private IPluginBundle bundle;
    private String effectName;

    
    public PluginTester(IPluginBundle bundle, String effectName) {
        this(null, bundle, effectName);
    }

    public PluginTester(List<WeakReference<Class<?>>> classRefs, IPluginBundle bundle, String effectName) {
        this.classRefs = classRefs;
        this.bundle = bundle;
        this.effectName = effectName;
    }
    
    private static TestSuite createTestSuite(String name) {
        if (Runtime.getRuntime().availableProcessors() > 1 &&
                Runtime.getRuntime().maxMemory() >= 5L * 1000 * 1000 * 1000)
            return new ActiveTestSuite(name);
        else
            return new TestSuite(name);
    }
    
    public TestSuite suite() {
        TestSuite suite = new TestSuite(effectName);
        {
            TestSuite serialSuite = createTestSuite("Single threaded");
            suite.addTest(serialSuite);
            {
                TestSuite scSuite = new TestSuite("Single channel");
                serialSuite.addTest(scSuite);
                LazyScPlugin plugin = new LazyScPlugin(bundle, effectName, false, null);
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
                LazyMcPlugin plugin = new LazyMcPlugin(bundle, effectName, chans, false, chans == 2 ? classRefs : null);
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 10, true, 1000));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10000, 10000, true, 3));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 10, 5000, true, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, false, 10));
                mcSuite.addTest(new PluginMcProcessingTest(plugin, 2048, 2048, true, 8));
            }
        }
        {
            TestSuite parallelSuite = createTestSuite("Multi threaded");
            suite.addTest(parallelSuite);
            {
                TestSuite scSuite = new TestSuite("Single channel");
                parallelSuite.addTest(scSuite);
                LazyScPlugin plugin = new LazyScPlugin(bundle, effectName, true, null);
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
                LazyMcPlugin plugin = new LazyMcPlugin(bundle, effectName, chans, true, null);
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

}
