package ch.tachyon.tunnel.host;

import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.effect.IPullEffect;
import ch.tachyon.tunnel.plugin.*;

/**
 * Utility class when you need to process raw audio data (either as <code>float[][]</code> or using callback-based audio sources)
 * using a known audio plugin that is in the classpath.
 * <p>
 * This class automatically handles several boilerplates (such as invoking {@link IEffect#load(IProcessingInfo)},
 * {@link IEffect#startProcessing(IProcessingInfo)} and other methods on the plugin) for you. On the other hand, it is
 * less flexible than using a plugin directly.
 * <p>
 * This class handles audio with any number of channels. If you only need monophonic audio (single channel), you might
 * want to use {@link MonoStreamProcessor} instead.
 * <p>
 * <b>Usage (with the {@link IMultiChanAudioSource} interface):</b>
 * <pre>
 * {@link IMultiChanAudioSource} myAudioSource = new IMultiChanAudioSource() {
 *     // Implement {@link IMultiChanAudioSource#getAudioFormat()}
 *     // and {@link IMultiChanAudioSource#readSamples(float[][])} to produce/synthezize/read your audio
 * };
 *     // or use AudioFileIO.createAudioSource() from the ch.tachyon.tunnel.audio package to create a source from an audio file or input stream
 * StreamProcessor processor = new StreamProcessor(44100.0f); // Replace with another sample rate if relevant
 * IEffect plugin = {@link #getPlugin(Class) processor.getPlugin(<i>plugin_class</i>)}; // Load a plugin
 * {@link IEffect#setParameterValue(ParameterDefinition, Object) plugin.setParameterValue(<i>plugin_class.parameter</i>, <i>value</i>)}; // Set zero or more parameters
 * {@link IMultiChanAudioSource} resultingAudio = {@link #filter(IMultiChanAudioSource, IPullEffect) processor.filter(source, plugin);} // Apply the effect
 * float[][] nextBlock = new float[<i>number_of_channels</i>][1024];
 * int read;
 * do {
 *     // Read the resulting audio chunk by chunk from the returned object
 *     // Note that the source audio is read and processed 'on demand' as you read from the returned object
 *     read = {@link IMultiChanAudioSource#readSamples(float[][]) resultingAudio.readSamples(nextBlock)};
 *     // Process the next chunk of the resulting audio.
 *     // It is in the 'nextBlock' array and contains 'read' sample frames
 * } while (read == 1024); // Only the last chunk will have less than the requested number of audio sample frames, and this is hence the condition for stopping
 * </pre>
 */
public class StreamProcessor implements IProcessingInfo {
    
    protected final static int MIN_CHUNK_LENGTH = 64;
    protected final static int PREFERRED_CHUNK_LENGTH = 2048;
    protected final static int MAX_CHUNK_LENGTH = 16384;
    
    protected final float sampleRate;
    protected final int nbChannels;
    protected final long length;
    protected double lengthChange = 1.0;
    protected int chunkLength = PREFERRED_CHUNK_LENGTH;
    protected BitSet channels;
    
    
    /**
     * Create a new stream processor. The length of the audio is assumed to be unknown
     */
    public StreamProcessor(AudioFormat format) {
        this.sampleRate = format.getSampleRate();
        this.nbChannels = format.getChannels();
        this.length = -1;
    }

    /**
     * Create a new stream processor
     * @param length the length of the audio stream to convert in sample frames (1 sample frames corresponds to
     * 1 <code>float</code> in mono, and n <code>float</code>s for n channels). Use <code>-1</code> if unknown
     */
    public StreamProcessor(AudioFormat format, long length) {
        this.sampleRate = format.getSampleRate();
        this.nbChannels = format.getChannels();
        this.length = length;
    }
    
    /**
     * Create a new stream processor.
     */
    public StreamProcessor(AudioFileFormat fileFormat) {
        this(fileFormat.getFormat(), fileFormat.getFrameLength());
    }
    
    /**
     * Load a plugin for processing audio
     * @param pluginClass the plugin class. Any class implementing {@link IPlugin} can be used
     * @return the plugin. You can set its parameters using {@link IEffect#setParameterValue(ParameterDefinition, Object)}
     * or {@link IEffect#setParameterValue(String, Object)}, and then process audio using {@link #filter(IMultiChanAudioSource, IPullEffect)}
     */
    public <E extends IPlugin> IPullEffect getPlugin(Class<E> pluginClass) {
        IPullEffect effect = EngineResolver.getEngine().getStaticPlugin(IPullEffect.class, pluginClass);
        effect.load(this);
        return effect;
    }
    
    /**
     * Filter the given audio stream using the given audio plugin. Reading from the returned source will read the
     * audio from the given source and transform it by the given audio plugin
     * <p>
     * The plugin will be released automatically when the last block is read from the returned source,
     * or if the returned source gets garbage collected.
     * @param source the audio source, from which the original audio is read
     * @param effect the audio plugin. Use {@link #getPlugin(Class)} to load a plugin
     * @return a new audio source, corresponding to the given source transformed by the given plugin
     */
    public IMultiChanAudioSource filter(final IMultiChanAudioSource source, final IPullEffect effect) {
        if (nbChannels != source.getAudioFormat().getChannels())
            throw new IllegalArgumentException("The numnber of channel in the source (" + source.getAudioFormat().getChannels() + ") does not match the number of channels of this StreamProcessor (" + nbChannels + ")");
        if (sampleRate != source.getAudioFormat().getSampleRate())
            throw new IllegalArgumentException("The sample rate of the source (" + source.getAudioFormat().getSampleRate() + ") does not match the sample rate of the StreamProcessor (" + sampleRate + ")");
        effect.startProcessing(this);
        this.lengthChange *= effect.getTimeChange();
        return new FilterSource(effect, source);
    }

    class FilterSource implements IMultiChanAudioSource, Closeable {

        private final IPullEffect effect;
        private final IMultiChanAudioSource source;
        private final float[][] buffer = new float[nbChannels][chunkLength];
        private int available;
        private int index = chunkLength;
        private boolean disposed = false;


        FilterSource(IPullEffect effect, IMultiChanAudioSource source) {
            this.effect = effect;
            this.source = source;
        }

        public int readSamples(float[][] target) {
            int remaining = target[0].length;
            int result = 0;
            while (remaining > 0) {
                if (index >= chunkLength) {
                    // Transform next block using effect and source
                    available = effect.process(source, buffer);
                    index = 0;
                }
                // Check how much we can copy and copy it
                int amount = Math.min(remaining, available - index);
                if (amount > 0) {
                    for (int c = 0; c < nbChannels; c++)
                        System.arraycopy(buffer[c], index, target[c], result, amount);
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

        public void close() {
            if (!disposed) {
                effect.stopProcessing();
                effect.unload();
                disposed = true;
            }
        }

        public AudioFormat getAudioFormat() {
            return source.getAudioFormat();
        }

        @Override
        protected void finalize() throws Throwable {
            close();
        }
    }

    public int getNumberOfChannels() {
        return nbChannels;
    }

    public BitSet getChannels() {
        if (channels == null) {
            channels = new BitSet();
            channels.set(0, nbChannels);
        }
        return channels;
    }

    public float getSampleRate() {
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
