package ch.tachyon.tritonus.test.wave;

import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        WaveFileTest.class,
        LargeWaveFileTest.class,
        HugeWaveFileTest.class,
})
public class WaveSuite {

}
