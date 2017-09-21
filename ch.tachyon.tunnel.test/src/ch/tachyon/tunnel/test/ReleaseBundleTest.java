package ch.tachyon.tunnel.test;

import java.util.*;

import ch.tachyon.tunnel.host.engine.*;


public class ReleaseBundleTest extends NoNewThreadTest {
    
    private IPluginBundle bundle;

    public ReleaseBundleTest(IPluginBundle bundle, Set<Long> threadIds) {
        super(ReleaseBundleTest.class.getSimpleName(), threadIds);
        this.bundle = bundle;
    }

    @Override
    protected void runTest() throws Throwable {
        bundle.releaseLoader();
        bundle = null;
        super.runTest();
        threadIds.clear();
        threadIds = null;
    }

}
