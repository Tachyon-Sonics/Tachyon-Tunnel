package ch.tachyon.tritonus.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ch.tachyon.tritonus.test.aiff.AiffSuite;
import ch.tachyon.tritonus.test.au.FastAuSuite;
import ch.tachyon.tritonus.test.wave.FastWaveSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AiffSuite.class,
        FastAuSuite.class,
        FastWaveSuite.class,
})
public class FastRootSuite {

}
