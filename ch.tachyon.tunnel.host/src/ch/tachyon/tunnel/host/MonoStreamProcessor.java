package ch.tachyon.tunnel.host;

import java.io.*;
import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.plugin.*;

/**
 * Utility class when you need to process mono raw audio data (as <code>float[]</code>) using a known audio plugin
 * that is in the classpath.
 * <p>
 * This class automatically handles several boilerplates (such as invoking {@link IEffect#load(IProcessingInfo)},
 * {@link IEffect#startProcessing(IProcessingInfo)} and other methods on the plugin) for you. On the other hand, it is
 * less flexible than using a plugin directly.
 * @see StreamProcessor
 */
public class MonoStreamProcessor implements IProcessingInfo {
    
    protected final static int MIN_CHUNK_LENGTH = 64;
    protected final static int PREFERRED_CHUNK_LENGTH = 2048;
    protected final static int MAX_CHUNK_LENGTH = 16384;
    
    protected double sampleRate;
    protected final long length;
    protected double lengthChange = 1.0;
    protected int chunkLength = PREFERRED_CHUNK_LENGTH;
    protected BitSet channels;
    
    
    /**
     * Create a new mono stream processor
     * @param length the length of the input audio stream to convert in sample frames (1 sample frames corresponds to
     * 1 <code>float</code> in mono, and n <code>float</code>s for n channels). Use <code>-1</code> if unknown
     */
    public MonoStreamProcessor(long length) {
        this.length = length;
    }
    
    /**
     * Create a new mono stream processor
     */
    public MonoStreamProcessor() {
        this(-1);
    }

    /**
     * Load a plugin for processing audio
     * @param pluginClass the plugin class. Any class implementing {@link IPlugin} can be used
     * @return the plugin. You can set its parameters using {@link IEffect#setParameterValue(ParameterDefinition, Object)}
     * or {@link IEffect#setParameterValue(String, Object)}, and then process audio using {@link #process(ISingleChanAudioSource, ISingleChanPullEffect, ISingleChanAudioSink)}
     */
    public <E extends IPlugin> ISingleChanPullEffect getPlugin(Class<E> pluginClass) {
        ISingleChanPullEffect effect = EngineResolver.getEngine().getStaticPlugin(ISingleChanPullEffect.class, pluginClass);
        effect.load(this);
        return effect;
    }
    
    /**
     * Process an audio stream using the given audio plugin
     * @param source the audio source, from which the original audio is read
     * @param effect the audio plugin. Use {@link #getPlugin(Class)} to load a plugin
     * @param target the audio sink, where the transformed audio is written
     */
    public void process(ISingleChanAudioSource source, ISingleChanPullEffect effect, ISingleChanAudioSink target) {
        this.sampleRate = source.getAudioFormat().getSampleRate();
        effect.startProcessing(this);
        try {
            float[] buffer = new float[chunkLength];
            
            int amount;
            do {
                amount = effect.process(source, buffer);
                if (amount == chunkLength) {
                    target.writeSamples(buffer);
                } else {
                    float[] tail = new float[amount];
                    System.arraycopy(buffer, 0, tail, 0, amount);
                    target.writeSamples(tail);
                }
            } while (amount == chunkLength);
        } finally {
            effect.stopProcessing();
            effect.unload();
        }
    }
    
    /**
     * Transform the given raw audio using the given audio plugin
     * <p>
     * While this method is friendly to use, it is less efficient in terms of memory and speed than
     * {@link #process(ISingleChanAudioSource, ISingleChanPullEffect, ISingleChanAudioSink)} or {@link #filter(ISingleChanAudioSource, ISingleChanPullEffect)}.
     * <p>
     * In particular, the entire audio to transform must be passed at once. Invoking this method multiple times with different consecutive fragments does not
     * give the same result because the effect is reset after each fragments (which are considered as individual files). With an echo effect for instance,
     * echoes from the previous fragment will <i>not</i> propagate to the next one.
     * <p>
     * Use {@link #filter(ISingleChanAudioSource, ISingleChanPullEffect)} or {@link #process(ISingleChanAudioSource, ISingleChanPullEffect, ISingleChanAudioSink)}
     * in order to properly process a big audio chunk using small fragments.
     * @param audio the original audio to transform. It is not modified by this method
     * @param effect the audio pugin
     * @return the resulting transformed raw audio
     */
    public float[] process(final float[] audio, final double sampleRate, ISingleChanPullEffect effect) {
        this.sampleRate = sampleRate;
        ISingleChanAudioSource source = new ISingleChanAudioSource() {
            
            private final int length = audio.length;
            private int offset = 0;
            
            public int readSamples(float[] target) {
                int requested = target.length;
                int amount = Math.min(requested, length - offset);
                if (amount > 0) {
                    System.arraycopy(audio, offset, target, 0, amount);
                    offset += amount;
                }
                return amount;
            }

            public AudioStreamFormat getAudioFormat() {
                return new AudioStreamFormat(sampleRate, 1, 32);
            }

            public void close() {
                // Nothing to do as we are reading from an array
            }
        };
        final List<float[]> outputs = new ArrayList<>();
        ISingleChanAudioSink sink = new ISingleChanAudioSink() {
            
            public void writeSamples(float[] samples) {
                float[] copy = new float[samples.length];
                System.arraycopy(samples, 0, copy, 0, samples.length);
                outputs.add(copy);
            }

            public void close() {
                // Nothing to do as we are just writing to an array
            }
        };
        process(source, effect, sink);
        int length = 0;
        for (float[] chunk : outputs)
            length += chunk.length;
        float[] output = new float[length];
        int offset = 0;
        for (float[] chunk : outputs) {
            int chunkLength = chunk.length;
            System.arraycopy(chunk, 0, output, offset, chunkLength);
            offset += chunkLength;
        }
        return output;
    }
    
    /**
     * Filter the given audio stream using the given audio plugin. Reading from the returned source will read the
     * audio from the given source after being transformed by the given audio plugin
     * <p>
     * The plugin will be released automatically when the last block is read from the returned source,
     * or if the returned source gets garbage collected.
     * @param source the audio source, from which the original audio is read
     * @param effect the audio plugin. Use {@link #getPlugin(Class)} to load a plugin
     * @return a new audio source, corresponding to the given source transformed by the given plugin
     */
    public ISingleChanAudioSource filter(final ISingleChanAudioSource source, final ISingleChanPullEffect effect) {
        this.sampleRate = source.getAudioFormat().getSampleRate();
        effect.startProcessing(this);
        this.lengthChange *= effect.getTimeChange();
        return new FilterSource(source, effect);
    }

    class FilterSource implements ISingleChanAudioSource, Closeable {
        
        private final ISingleChanAudioSource source;
        private final ISingleChanPullEffect effect;
        private final float[] buffer = new float[chunkLength];
        private int available;
        private int index = chunkLength;
        private boolean disposed = false;

        private FilterSource(ISingleChanAudioSource source, ISingleChanPullEffect effect) {
            this.source = source;
            this.effect = effect;
        }

        public int readSamples(float[] target) {
            int remaining = target.length;
            int result = 0;
            while (remaining > 0) {
                if (index >= chunkLength) {
                    // Transform next block using supplied effect and source
                    available = effect.process(source, buffer);
                    index = 0;
                }
                // Check how much we can copy and copy it
                int amount = Math.min(remaining, available - index);
                if (amount > 0) {
                    System.arraycopy(buffer, index, target, result, amount);
                    // Update indexes
                    index += amount;
                    result += amount;
                    remaining -= amount;
                }
                if (index >= available && available < chunkLength) {
                    // This was the last block
                    close();
                    break; // This was the last block. We cannot read more
                }
            }
            return result;
        }

        public AudioStreamFormat getAudioFormat() {
            return source.getAudioFormat();
        }

        public void close() {
            if (!disposed) {
                effect.stopProcessing();
                effect.unload();
                disposed = true;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            close();
        }
    }

    /**
     * Overriden to return <code>1</code> as we only support monophonic audio.
     * <p>
     * If you need to process stereo or multi-channel audio, use {@link StreamProcessor} instead.
     */
    public int getNumberOfChannels() {
        return 1;
    }

    public BitSet getChannels() {
        if (channels == null) {
            channels = new BitSet();
            channels.set(0);
        }
        return channels;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    /**
     * Quantization. We return 24 to match the full precision of Java's <code>float</code>.
     * <p>
     * Few plugins really require this, but some do. You may override to return 16 if CD quality
     * is enough (it might speed up some plugins)
     */
    public int getQuantization() {
        return 24;
    }

    public int getMaxChunkLength(int pluginOptimalChunkLength) {
        return chunkLength;
    }

    /**
     * Our preferred chunk length. Note that we will accept any preferred length from the plugin
     * as long as it is between the min and max.
     * <p>
     * If low latency is required (at the expense of performance and parallelization on multi-cores),
     * you may change or override the code to return {@link #MIN_CHUNK_LENGTH} instead.
     * <p>
     * If best performances are required (at the expense of latency, which is corrected anyway), you
     * may change or override the code to return {@link #MAX_CHUNK_LENGTH} instead.
     */
    public int getHostPreferredChunkLength() {
        return PREFERRED_CHUNK_LENGTH;
    }

    public int negociateFixedChunkLength(int pluginOptimalChunkLength) {
        if (pluginOptimalChunkLength < MIN_CHUNK_LENGTH || pluginOptimalChunkLength > MAX_CHUNK_LENGTH)
            chunkLength = PREFERRED_CHUNK_LENGTH;
        else
            chunkLength = pluginOptimalChunkLength;
        return chunkLength;
    }

    public long getLength() {
        return length;
    }

    /**
     * The number of sample frame of the resulting audio stream.
     * This is the same as {@link #getLength()}, unless plugins that change the audio speed (resampling, time stretching) are involved
     */
    public long getOutputLength() {
        return (long) ((double) length * lengthChange + 0.5);
    }

}
