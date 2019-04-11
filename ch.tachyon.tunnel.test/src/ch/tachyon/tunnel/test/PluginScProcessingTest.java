package ch.tachyon.tunnel.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import junit.framework.*;

public class PluginScProcessingTest extends TestCase {

    private final static float MIN_RATIO_TIME_CHANGE = 0.99f;
    private final static float MAX_RATIO_TIME_CHANGE = 1.01f;
    private final static float MAX_FRAMES_TIME_CHANGE = 10.0f;

    private final LazyScPlugin plugin;
    private final int minSize;
    private final int maxSize;
    private final boolean cleanup;
    private final int count;
    
    private final Random rnd = new Random();
    
    
    public PluginScProcessingTest(LazyScPlugin plugin, int minSize, int maxSize, boolean cleanup, int count) {
        super(getName(plugin.getEffectName(), plugin.isParallel(), minSize, maxSize, cleanup, count));
        this.plugin = plugin;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.cleanup = cleanup;
        this.count = count;
    }

    private static String getName(String effectName, boolean parallel, int minSize, int maxSize, boolean cleanup, int count) {
        StringBuilder result = new StringBuilder();
        result.append(effectName);
        result.append(": Mono");
        if (parallel)
            result.append(", parallel");
        if (minSize == maxSize) {
            result.append(", size " + minSize);
        } else {
            result.append(", varying size");
        }
        if (!cleanup)
            result.append(", with aborting");
        result.append(", " + count + " audio chunks");
        return result.toString();
    }


    @Override
    protected void runTest() throws ClassNotFoundException {
        ISingleChanPushEffect plugin = this.plugin.getPlugin();
        if (plugin != null) {
            runEffect(plugin);
        }
        this.plugin.release();
    }
    
    private void runEffect(ISingleChanPushEffect plugin) {
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
    

}
