package ch.tachyon.tritonus.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ch.tachyon.tritonus.test.aiff.AiffSuite;
import ch.tachyon.tritonus.test.au.AuSuite;
import ch.tachyon.tritonus.test.wave.WaveSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AiffSuite.class,
	AuSuite.class,
    WaveSuite.class,
})
public class RootSuite {

}
