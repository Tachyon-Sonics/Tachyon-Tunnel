package ch.tachyon.tunnel.test;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import ch.tachyon.tunnel.host.Loader;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.IPluginBundle;

/**
 * Helper to create a test suite using your plugin bundle.
 * <p>
 * Usage:
 * <pre>
{@literal @}RunWith(AllTests.class)
public class BundleTest {

    public static TestSuite suite() throws IOException {
        File bundleFile = new File("<i>patch_to_eclipse_project_with_all_your_effects</i>/bin"); // Or path to your plugin bundle .jar file
        URL bundleUrl = bundleFile.toURI().toURL();
        PluginBundleTester tester = new PluginBundleTester(bundleUrl);
        return tester.suite();
    }

}
 * </pre>
 */
public class PluginBundleTester {
    
    private final URL bundleUrl;
    private final Set<String> exclusions = new HashSet<>();
    
    
    public PluginBundleTester(URL bundleUrl) {
        this.bundleUrl = bundleUrl;
    }
    
    /**
     * @param pluginName full class name of plugin to exclude
     */
    public void addExclusion(String pluginName) {
        this.exclusions.add(pluginName);
    }
    
    public TestSuite suite() throws IOException {
        // Ensure assertions are enabled
        try {
            assert false;
            throw new IllegalStateException("\nAssertions are not enabled. Please enable them by specifying the -ea argument to the JVM.\n" +
                        "Most tests are based on assertions and hence if assertions are disabled, problems will not be detected correctly.");
        } catch (AssertionError ex) {
            // Ok, assertions enabled
        }

        // Load bundle
        Set<Long> threadIds = new HashSet<Long>();
        List<String> effectNames = getAllPlugins(bundleUrl);
        effectNames.removeAll(exclusions);
        List<WeakReference<Class<?>>> classRefs = new ArrayList<WeakReference<Class<?>>>();
        
        // Create the suite
        IPluginBundle bundle = Loader.loadPluginBundle(bundleUrl, true);
        TestSuite suite = new TestSuite(bundle.getName());
        suite.addTest(new NoNewThreadTest(threadIds));
        for (String effectName : effectNames) {
            PluginTester tester = new PluginTester(classRefs, bundle, effectName);
            suite.addTest(tester.suite());
        }
        suite.addTest(new ReleaseBundleTest(bundle, threadIds));
        suite.addTest(new MemoryLeakTest(classRefs));
        return suite;
    }

    private List<String> getAllPlugins(URL bundleUrl) throws IOException {
        List<String> effectNames = new ArrayList<String>();
        IPluginBundle bundle = Loader.loadPluginBundle(bundleUrl, true);
        List<Class<? extends IEffect>> hostInterfaces = new ArrayList<Class<? extends IEffect>>();
        hostInterfaces.add(ISingleChanPushEffect.class);
        hostInterfaces.add(IPushEffect.class);
        for (IEffect effect : bundle.loadAllPlugins(hostInterfaces))
            effectNames.add(effect.getClassName());
        bundle.releaseLoader();
        bundle = null;
        return effectNames;
    }

}
