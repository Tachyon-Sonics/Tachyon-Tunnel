package ch.tachyon.tunnel.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;
import junit.framework.*;

public class PluginMcProcessingTest extends TestCase {

    private final static float MIN_RATIO_TIME_CHANGE = 0.99f;
    private final static float MAX_RATIO_TIME_CHANGE = 1.01f;
    private final static float MAX_FRAMES_TIME_CHANGE = 10.0f;

    private final LazyMcPlugin plugin;
    private final int nbChannels;
    private final int minSize;
    private final int maxSize;
    private final boolean cleanup;
    private final int count;
    
    private final Random rnd = new Random();
    
    
    public PluginMcProcessingTest(LazyMcPlugin plugin, int minSize, int maxSize, boolean cleanup, int count) {
        super(getName(plugin.getEffectName(), plugin.getNbChannels(), plugin.isParallel(), minSize, maxSize, cleanup, count));
        this.plugin = plugin;
        this.nbChannels = plugin.getNbChannels();
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.cleanup = cleanup;
        this.count = count;
    }
    
    private static String getName(String effectName, int nbChannels, boolean parallel, int minSize, int maxSize, boolean cleanup, int count) {
        StringBuilder result = new StringBuilder();
        result.append(effectName);
        result.append(": " + nbChannels + " channels");
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
        IPushEffect plugin = this.plugin.getPlugin();
        if (plugin != null) {
            int minNbChan = 1;
            int maxNbChan = 3;
            Channels channels = plugin.getAnnotation(Channels.class);
            if (channels != null) {
                minNbChan = Math.max(minNbChan, channels.min());
                maxNbChan = Math.min(maxNbChan, channels.max());
            }
            if (nbChannels >= minNbChan && nbChannels <= maxNbChan)
                runMcEffect(plugin);
        }
        this.plugin.release();
    }
    
    private void runMcEffect(IPushEffect plugin) {
        ProcessingInfo info = new ProcessingInfo(nbChannels, 44100.0f, 16, -1, maxSize, (minSize == maxSize ? maxSize : -1));
        plugin.load(info);
        plugin.startProcessing(info);
        double timeChange = plugin.getTimeChange();
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
                    double expected = nbInput * timeChange;
                    double ratio = expected / nbOutput.get();
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
