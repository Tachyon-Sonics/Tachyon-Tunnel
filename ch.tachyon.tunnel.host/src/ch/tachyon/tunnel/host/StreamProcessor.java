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
 *     
 * StreamProcessor processor = new StreamProcessor(44100.0f, 2, -1); // Replace with another sample rate if relevant
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
 * 
 * See {@link #setCallbackInfo(long, Runnable)} and {@link #getProgressPercent()} for a way of monitoring the current progress.
 * <p>
 * To save the result to a {@link File} or to an {@link OutputStream}, use methods from  <tt>AudioFileIO</tt>.
 * <p>
 * To simply process a file os stream with a single plugin, use <tt>SimpleProcessor</tt>.
 */
public class StreamProcessor {
    
    protected final static int MIN_CHUNK_LENGTH = 64;
    protected final static int PREFERRED_CHUNK_LENGTH = 2048;
    protected final static int MAX_CHUNK_LENGTH = 16384;
    
    private final double sampleRate;
    private final int nbChannels;
    private final long length;
    private final ProcessingInfo processingInfo = new ProcessingInfo();
    private double lengthChange = 1.0;
    private int chunkLength = PREFERRED_CHUNK_LENGTH;
    private BitSet channels;
    
    // Progress monitoring
    private boolean terminated;
    private long inputLength;
    private Runnable callback;
    private IMultiChanAudioSource mainSource;
    private List<IPullEffect> effects = new ArrayList<>();
    private Long additionalInputFrames; // Cached value
    
    
    /**
     * Create a new stream processor. The length of the audio is assumed to be unknown
     * @param format the audio format. Only sample rate and number of channels are relevant
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
     * Create a new stream processor
     * @param length the length of the audio stream to convert in sample frames (1 sample frames corresponds to
     * 1 <code>float</code> in mono, and n <code>float</code>s for n channels). Use <code>-1</code> if unknown
     */
    public StreamProcessor(double sampleRate, int nbChannels, long length) {
        this.sampleRate = sampleRate;
        this.nbChannels = nbChannels;
        this.length = length;
    }
    
    /**
     * Create a new stream processor.
     * <p>
     * You may want to use <tt>AudioFilesTypes.instance().getAudioFileFormat()</tt> in ch.tachyon.tunnel.audio
     */
    public StreamProcessor(AudioFileFormat fileFormat) {
        this(fileFormat.getFormat(), fileFormat.getFrameLength());
    }
    
    /**
     * Specify a callback that must be invoked at regular intervals while processing the stream.
     * <p>
     * There is no guarantee on how frequently the callback will be invoked, as this depends on the buffer sizes
     * of the used effects as well as internal buffer sizes. In general this is invoked at least each time
     * audio is read from the {@link IMultiChanAudioSource} returned by {@link #filter(IMultiChanAudioSource, IPullEffect)}.
     * <p>
     * A typical use of the the callback is to invoke {@link #getProgressPercent()} in order to monitor the current progress.
     * Note though that the callback might be called very frequently.
     * <p>
     * The callback is allowed to throw an <tt>ch.tachyon.tunnel.utils.OperationAbortedException</tt> to cleanly abort the processing
     * at any time.
     * @param audioStreamLengthInFrames (optional) the length of the audio that will be processed, in frames. Unless correctly specified,
     * {@link #getProgressPercent()} will not work. Note: a 'frame' consists of a sample value in all channels, e.g. in stereo
     * one frame = two samples. Use -1 if not used or not known.
     * @param callback the callback to invoke, as the {@link Runnable#run()} method.
     */
    public void setCallbackInfo(long audioStreamLengthInFrames, Runnable callback) {
        this.inputLength = audioStreamLengthInFrames;
        this.callback = callback;
    }

    /**
     * Load a plugin for processing audio
     * @param pluginClass the plugin class. Any class implementing {@link IPlugin} can be used
     * @return the plugin. You can set its parameters using {@link IEffect#setParameterValue(ParameterDefinition, Object)}
     * or {@link IEffect#setParameterValue(String, Object)}, and then process audio using {@link #filter(IMultiChanAudioSource, IPullEffect)}
     */
    public <E extends IPlugin> IPullEffect getPlugin(Class<E> pluginClass) {
        IPullEffect effect = EngineResolver.getEngine().getStaticPlugin(IPullEffect.class, pluginClass);
        effect.load(processingInfo);
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
    public IMultiChanAudioSource filter(IMultiChanAudioSource source, final IPullEffect effect) {
        if (nbChannels != source.getAudioFormat().getChannels())
            throw new IllegalArgumentException("The numnber of channel in the source (" + source.getAudioFormat().getChannels() + ") does not match the number of channels of this StreamProcessor (" + nbChannels + ")");
        if (sampleRate != source.getAudioFormat().getSampleRate())
            throw new IllegalArgumentException("The sample rate of the source (" + source.getAudioFormat().getSampleRate() + ") does not match the sample rate of the StreamProcessor (" + sampleRate + ")");
        effect.startProcessing(processingInfo);
        this.lengthChange *= effect.getTimeChange();
        if (mainSource == null) {
            source = new MainSourceWrapper(source); // Wrap to help monitoring progress
            mainSource = source;
        }
        terminated = false;
        effects.add(effect);
        additionalInputFrames = null;
        return new FilterSource(effect, source);
    }
    
    /**
     * Get the current processing progress in percent.
     * <p>
     * The result in generally more reliable than using a progress in AudioFileIO.createAudioSource().
     * However, this method will not work (and return {@link Double#NaN}) if the length of the input file
     * is not known.
     * <p>
     * This can be invoked by a callback specified by {@link #setCallbackInfo(long, Runnable)}
     */
    public double getProgressPercent() {
        if (terminated)
            return 100.0;
        long toProcess = inputLength;
        if (toProcess == AudioSystem.NOT_SPECIFIED)
            return Double.NaN;
        if (effects.isEmpty())
            throw new IllegalStateException();
        if (additionalInputFrames == null) {
            additionalInputFrames = 0L;
            for (IPullEffect effect : effects)
                additionalInputFrames += effect.getAdditionalInputFrames();
        }
        toProcess += additionalInputFrames;
        
        long processed = 0;
        for (IPullEffect effect : effects)
            processed += effect.getProcessedFrames();
        processed /= effects.size();
        
        return (double) processed * 100.0 / (double) toProcess;
    }
    
    /**
     * Wrapper around {@link IMultiChanAudioSource} that invokes the callback on every reads.
     * This improves the accuracy of {@link StreamProcessor#getProgressPercent()} by invoking
     * the callback more often.
     * <p>
     * For accurate progress monitoring, it is indeed necessary to invoke the callback at least when:
     * <ul>
     * <li>Audio is read from the first (untransformed) source
     * <li>Audio is read from the last (fully transformed) source
     * </ul>
     * The reason is that an effect might
     * <ul>
     * <li>Read a lot of sample frames before producing the first ones
     * <li>Write a lot of sample frames after the last one has been read
     * </ul>
     */
    class MainSourceWrapper implements IMultiChanAudioSource, Closeable {
        
        private final IMultiChanAudioSource target;
        
        
        public MainSourceWrapper(IMultiChanAudioSource target) {
            this.target = target;
        }

        public AudioFormat getAudioFormat() {
            return target.getAudioFormat();
        }

        public int readSamples(float[][] data) {
            int result = target.readSamples(data);
            if (callback != null)
                callback.run();
            return result;
        }

        public void close() {
            target.close();
        }
        
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
                if (callback != null)
                    callback.run();
            }
            return result;
        }

        public void close() {
            closeImpl(true);
        }

        private void closeImpl(boolean normalTermination) {
            if (!disposed) {
                effect.stopProcessing();
                effect.unload();
                terminated = true;
                source.close();
                if (callback != null)
                    callback.run();
                disposed = true;
            }
        }

        public AudioFormat getAudioFormat() {
            return source.getAudioFormat();
        }

        @Override
        protected void finalize() throws Throwable {
            closeImpl(false);
        }
    }
    
    /**
     * Get the preferred chunk length in sample frames.
     * <p>
     * Reading audio chunks of the returned length will result in optimal performances.
     */
    public int getPreferredChunkLength() {
        return chunkLength;
    }

    public long getLength() {
        return length;
    }

    /**
     * The number of sample frame of the resulting audio stream.
     * This is the same as the input length, unless plugins that change the audio speed (resampling, time stretching) are involved
     */
    public long getOutputLength() {
        if (length == -1)
            return -1;
        return (long) ((double) length * lengthChange + 0.5);
    }
    
    /**
     * An {@link IProcessingInfo} implementation based on this processor
     */
    public IProcessingInfo getProcessingInfo() {
        return this.processingInfo;
    }
    
    class ProcessingInfo implements IProcessingInfo {

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

        /**
         * Do not use. IProcessingInfo implementation
         */
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

    }

}
