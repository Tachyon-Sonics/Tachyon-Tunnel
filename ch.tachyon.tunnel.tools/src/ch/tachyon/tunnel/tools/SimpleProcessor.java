package ch.tachyon.tunnel.tools;

import java.io.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.*;
import javax.sound.sampled.AudioFormat.*;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.IPullEffect;
import ch.tachyon.tunnel.plugin.*;

/**
 * Static utilities to transform an audio file or input stream using a single effect.
 * <p>
 * Example of usage:
 * <pre>
 * IPullEffect effect = SimpleProcessor.{@link #getPlugin(Class)};
 * effect.{@link IPullEffect#setParameterValue(String, Object) setParameterValue("parameterName", value)};
 * effect.{@link IPullEffect#setParameterValue(String, Object) setParameterValue("parameterName", value)};
 *  ...
 * File inputFile = new File("myAudio.mp3"); // an existing audio file
 * File outputFile = new File("result.wav"); // &lt;- file extension determines output file format
 * SimpleProcessor.{@link #process(File, IPullEffect, File) process(inputFile, effect, outputFile});
 * </pre>
 * Last line can also use streams, but in that case the input and output formats must be specified:
 * <pre>
 * SimpleProcessor.{@link #process(String, InputStream, IPullEffect, OutputStream, Type) process(".mp3", inputStream, effect, outputStream, AudioFileFormat.Type.AU});
 * </pre>
 * <p>
 * Multiple variants of <tt>process()</tt> are available, either with {@link File} or with {@link InputStream}/{@link OutputStream}.
 * Due to the nature of the Java audio system, {@link File}s are always preferred over streams.
 * <p>
 * In particular, when using an {@link InputStream}, it is necessary to specify the file name so that the proper decoder can be found
 * (specifying an extension like "<tt>.mp3</tt>" is sufficient).
 * <p>
 * When saving to an {@link OutputStream}, the length must be known in advance, unless the {@link Type#AU} file type is used.
 * The length can always be computed when the input is a {@link File}, but it might be unavailable when using an {@link InputStream}.
 * <p>
 * <b>Warning: If both the input and output are specified as streams, and if the length of the input cannot be known in advance and the output file
 * format does not allow unknown length, an exception will be thrown.</b>
 * A typical case is when transforming from mp3 to wav, both using stream. To prevent this problem, either
 * <ul>
 * <li>Use a File for either the input or the output, or both
 * <li>Use an input format whose length is specified at the beginning of the file, such as .wav or .aiff
 * <li>Use an output format that allows unknown length, such as .au ({@link Type#AU})
 * </ul>
 * <p>
 * For more advanced processing, such as chaining multiple effects or capturing the result, use {@link StreamProcessor}.
 * <p>
 * This class supports a limited way of chaining effects. When used, the effects are processed concurrently using multiple threads.
 * Here's an example of chaining 3 effects:
 * <pre>
        File inputFile = new File("myAudio.wav");
        File outputFile = new File("result.wav");

        IPullEffect effect1 = SimpleProcessor.{@link #getPlugin(Class)};
        // Set parameters of effect1 here
        InputStream processed1 = SimpleProcessor.process(inputFile, effect1, Type.WAVE);

        IPullEffect effect2 = SimpleProcessor.{@link #getPlugin(Class)};
        // Set parameters of effect2 here
        InputStream processed2 = SimpleProcessor.process(".wav", processed1, effect2, Type.WAVE);
            // ".wav" above must match Type.WAVE of previous processing. This is the internal file format used between effects
        
        // Repeat the above block to chain more effects

        IPullEffect effect3 = SimpleProcessor.{@link #getPlugin(Class)};
        // Set parameters of effect3 here
        SimpleProcessor.process(".wav", processed2, effect3, outputFile, null);
 * </pre>
 * This is however not as efficient as using {@link StreamProcessor}, because an explicit conversion to a file format (WAVE is this
 * example) is performed between the effects.
 */
public class SimpleProcessor {

    /**
     * Load an audio effect. The resulting {@link IPullEffect} can be used in one of the {@link #process(File, IPullEffect, File)}
     * methods to process a file.
     * <p>
     * Use {@link IPullEffect#setParameterValue(String, Object)} to set the audio effect's parameters before processing.
     * The name of the parameters and their possible values depend on the actual audio effect.
     * @param pluginClass the class of the effect to load
     */
    public static <E extends IPlugin> IPullEffect getPlugin(Class<E> pluginClass) {
        IPullEffect effect = StaticLoader.getPlugin(IPullEffect.class, pluginClass);
        return new LazyPullEffect(effect);
    }

    private static AudioFileFormat getOutputFormat(AudioFileFormat.Type outputFileType, AudioFileFormat inputFormat, StreamProcessor processor) {
        Map<String, Object> properties = inputFormat.properties();
        if (properties == null)
            properties = new HashMap<>();
        AudioFileFormat format = new AudioFileFormat(outputFileType,
                new AudioFormat(
                        inputFormat.getFormat().getSampleRate(),
                        coalesce(inputFormat.getFormat().getSampleSizeInBits(), 16),
                        coalesce(inputFormat.getFormat().getChannels(), 2),
                        true, true),
                (int) processor.getOutputLength(),
                properties);
        return format;
    }

    private static AudioFileFormat getOutputFormat(AudioFileFormat outputFormat, AudioFileFormat inputFormat, StreamProcessor processor) {
        Map<String, Object> properties = inputFormat.properties();
        if (properties == null)
            properties = new HashMap<>();
        AudioFileFormat format = new AudioFileFormat(outputFormat.getType(),
                new AudioFormat(
                        coalesce(outputFormat.getFormat().getSampleRate(), inputFormat.getFormat().getSampleRate()),
                        coalesce(outputFormat.getFormat().getSampleSizeInBits(), coalesce(inputFormat.getFormat().getSampleSizeInBits(), 16)),
                        coalesce(outputFormat.getFormat().getChannels(), coalesce(inputFormat.getFormat().getChannels(), 2)),
                        outputFormat.getFormat().getEncoding() != Encoding.PCM_UNSIGNED,
                        outputFormat.getFormat().isBigEndian()),
                (int) processor.getOutputLength(),
                properties);
        return format;
    }

    private static int coalesce(int value, int dflt) {
        if (value == AudioSystem.NOT_SPECIFIED)
            return dflt;
        return value;
    }

    private static float coalesce(float value, float dflt) {
        if (value == AudioSystem.NOT_SPECIFIED)
            return dflt;
        return value;
    }

    /**
     * Process audio from the given input file using the given audio effect, and save the result in the given output file using the given
     * file format type.
     * <p>
     * Note: if the input file does not contain a header specifying its length, it will be read entirely a first time before processing, in order to
     * compute its length.
     * @param input the input audio file
     * @param effect the effect to apply
     * @param output the output file. Its extension (such as "<tt>.wav</tt>") is used to infer the output file format. Must be different than
     * the input file
     * @throws IOException if there was an error reading or writing
     * @throws UnsupportedAudioFileException if the input audio file format is not supported
     */
    public static void process(File input, IPullEffect effect, File output) throws IOException, UnsupportedAudioFileException {
        process(input, effect, output, null, null);
    }

    public static void process(File input, IPullEffect effect, File output, final IProgressMonitor progress) throws IOException, UnsupportedAudioFileException {
        process(input, effect, output, null, progress);
    }
    
    /**
     * @param outputFormat if non-null, the output file format. Any value of {@link AudioSystem#NOT_SPECIFIED} will be replaced by the
     * corresponding value of the input file format. The output length is always overriden with the computed output length, based on the
     * applied effect (usually the same as the input length, except for effects changing the audio duration).
     */
    public static void process(File input, IPullEffect effect, File output, AudioFileFormat outputFormat, final IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(input, true);
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(input);
        final StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        if (outputFormat == null) {
            AudioFileFormat.Type outputFileType = AudioFileTypes.instance().getWritableType4ext(output.getName());
            if (outputFileType == null)
                throw new UnsupportedOperationException("Cannot infer the output audio file type from the file name: " + output.getAbsolutePath());
            outputFormat = getOutputFormat(outputFileType, inputFormat, processor);
        } else {
            outputFormat = getOutputFormat(outputFormat, inputFormat, processor);
        }
        AudioFileIO.saveAudioFile(transformedSource, output, outputFormat, null);
        if (progress != null)
            progress.setProgress(100.0, 100.0); // Ensure progress is closed
    }

    private static void setupProgressUsingProcessor(final IProgressMonitor progress, AudioFileFormat inputFormat, final StreamProcessor processor) {
        if (progress != null) {
            processor.setCallbackInfo(inputFormat.getFrameLength(), new Runnable() {

                @Override
                public void run() {
                    progress.setProgress(processor.getProgressPercent(), 100.0);
                }
            });
        }
    }

    public static void process(File input, IPullEffect effect, OutputStream output, AudioFileFormat.Type outputFileType) throws IOException, UnsupportedAudioFileException {
        process(input, effect, output, outputFileType, null);
    }

    public static void process(File input, IPullEffect effect, OutputStream output, AudioFileFormat.Type outputFileType, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(input, true);
        if (outputFileType == null) {
            outputFileType = inputFormat.getType();
            if (!Arrays.asList(new Type[] { Type.WAVE, Type.AIFF, Type.AIFC, Type.AU}).contains(outputFileType))
                outputFileType = Type.WAVE;
        }
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(input);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileType, inputFormat, processor);
        AudioFileIO.saveAudioFile(transformedSource, output, format, progress);
    }

    /**
     * Process audio from the given input stream using the given audio effect, and write the result to the given output stream using the given
     * file format type.
     * @param inputFileName a file name, or an extension, that identifies the audio format of the input stream. For example ".wav" or ".aiff".
     * @param input the input stream. The format must be an audio file format (not just raw encoded audio) that matches the extension of the specified
     * input file name. It is closed by this method after the processing.
     * @param effect the effect to apply to the audio
     * @param output the output stream in which to write the transformed audio. It is closed by this method after the processing.
     * @param outputFileType the output audio file format)
     */
    public static void process(String inputFileName, InputStream input, IPullEffect effect, OutputStream output, AudioFileFormat.Type outputFileType) throws IOException, UnsupportedAudioFileException {
        process(inputFileName, input, effect, output, outputFileType, null);
    }

    public static void process(String inputFileName, InputStream input, IPullEffect effect, OutputStream output, AudioFileFormat.Type outputFileType, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFileName, input);
        if (outputFileType == null) {
            if (inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED) {
                outputFileType = Type.AU;
            } else if (Arrays.asList(new Type[] { Type.WAVE, Type.AIFF, Type.AIFC, Type.AU}).contains(inputFormat.getType())) {
                outputFileType = inputFormat.getType();
            } else {
                outputFileType = Type.WAVE;
            }
        }
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFileName, input);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        if (inputFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED)
            setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileType, inputFormat, processor);
        AudioFileIO.saveAudioFile(transformedSource, output, format,
                inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED ? progress : null);
    }

    public static void process(String inputFileName, InputStream input, IPullEffect effect, File outputFile, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat.Type outputFileType = fromFileName(outputFile.getName());
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFileName, input);
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFileName, input);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        if (inputFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED)
            setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileType, inputFormat, processor);
        AudioFileIO.saveAudioFile(transformedSource, outputFile, format,
                inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED ? progress : null);
    }

    /**
     * Process audio from the given input stream using the given audio effect, and return the result as a readable input stream using the given
     * file format type. The resulting {@link InputStream} can be passed to further calls to this method in order to chain effects.
     * <p>
     * Not that due to the was the Java audio subsystem work (it can only save to an output stream), this method will create a helper Thread
     * in order to use {@link PipedInputStream} / {@link PipedOutputStream}. On the other hand, when multiple effects are chained using
     * this method, they will benefit from multi-threading (each effect is processed concurrently by its own thread).
     * @param inputFileName a file name, or an extension, that identifies the audio format of the input stream. For example ".wav" or ".aiff".
     * @param input the input stream. The format must be an audio file format (not just raw encoded audio) that matches the extension of the specified
     * input file name
     * @param effect the effect to apply to the audio
     * @param outputFileType the output audio file format)
     * @return an input stream with the resulting transformed audio. Audio is transformed lazily, as bytes are read from the input stream. Closing
     * the returned input stream closes the 'input' input stream passed as argument
     */
    public static InputStream process(String inputFileName, InputStream input, IPullEffect effect, AudioFileFormat.Type outputFileType) throws IOException, UnsupportedAudioFileException {
        return process(inputFileName, input, effect, outputFileType, null);
    }

    public static InputStream process(String inputFileName, InputStream input, IPullEffect effect, AudioFileFormat.Type outputFileType, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFileName, input);
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFileName, input);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        if (inputFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED)
            setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileType, inputFormat, processor);
        return AudioFileIO.convertToFormat(transformedSource, format,
                inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED ? progress : null);
    }

    public static InputStream process(File inputFile, IPullEffect effect, AudioFileFormat.Type outputFileType) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        return process(inputFile, effect, outputFileType, null);
    }
    
    public static InputStream process(File inputFile, IPullEffect effect, AudioFileFormat.Type outputFileType, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFile, true);
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFile);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        if (inputFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED)
            setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileType, inputFormat, processor);
        return AudioFileIO.convertToFormat(transformedSource, format,
                inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED ? progress : null);
    }

    /**
     * @param inputFileName name of the input file, used to determine its format. It is sufficient to specify an extension (including the dot),
     * such as "<tt>.wav</tt>" or "<tt>.mp3</tt>".
     * @param input
     * @param effect
     * @param outputFileFormat the output audio file format. Values that are set to {@link AudioSystem#NOT_SPECIFIED} are replaced by those of
     * the input file format.
     * @param progress an optional {@link IProgressMonitor} implementation. The method {@link IProgressMonitor#setProgress(double, double)}
     * will be invoked regularly so the progress can be monitored. Use <tt>null</tt> if no progress monitoring is necessary. The given object
     * should not make any assumption on the frequency at which {@link IProgressMonitor#setProgress(double, double)} will be invoked.
     * @throws OperationAbortedException if the given progress is not null, and {@link IProgressMonitor#setProgress(double, double)} threw
     * an {@link OperationAbortedException} while transforming the file
     * @throws IOException
     * @throws IllegalArgumentException if the input audio stream does not specify its length, and the output file format does not allow
     * unknown length. Note that {@value Type#AU} supports unknown length and is always available.
     * @throws UnsupportedAudioFileException if the input or output audio file format is not supported
     */
    public static InputStream process(String inputFileName, InputStream input, IPullEffect effect, AudioFileFormat outputFileFormat, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFileName, input);
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFileName, input);
        StreamProcessor processor = new StreamProcessor(FormatUtils.toStreamFormat(inputFormat.getFormat()), inputFormat.getFrameLength());
        effect.load(processor.getProcessingInfo());
        if (inputFormat.getFrameLength() != AudioSystem.NOT_SPECIFIED)
            setupProgressUsingProcessor(progress, inputFormat, processor);
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, effect);
        AudioFileFormat format = getOutputFormat(outputFileFormat, inputFormat, processor);
        return AudioFileIO.convertToFormat(transformedSource, format,
                inputFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED ? progress : null);
    }

    public static void process(InputFileOrStream input, IPullEffect effect, OutputFileOrStream output) throws IOException, UnsupportedAudioFileException {
        process(input, effect, output, null);
    }

    private static Type fromFileName(String fileName) {
        Type[] types = new Type[] {Type.WAVE, Type.AIFF, Type.AIFC, Type.AU};
        for (Type type : types) {
            if (fileName.endsWith("." + type.getExtension()))
                return type;
        }
        if (fileName.endsWith(".aiff"))
            return Type.AIFF;
        return Type.WAVE;
    }

    /**
     * Transform the given file or stream using the given effect, saving the result to the given file or stream
     * <p>
     * If the output is a file, its extension determines the file format (.wav, .aiff, etc)
     * <p>
     * If the output is a stream:
     * <li>If the input is a file, the output file format will be the same as the input if it is supported (WAVE, AIFF/C or AU); or WAVE else
     * <li>If the input is a stream, the output file format will be the same as the input if it is supported (WAVE, AIFF/C or AU) <i>and</i> if the
     * length can be known immediately. If the format is unsupported but the length is known, WAVE is used, else AU is used.
     * The length can generally be known immediately from a stream of WAVE or AIFF/C format, but not from compressed streams such as mp3.
     */
    public static void process(InputFileOrStream input, IPullEffect effect, OutputFileOrStream output, IProgressMonitor progress) throws OperationAbortedException, IOException, UnsupportedAudioFileException {
        if (input.isFile()) {
            if (output.isFile()) {
                process(input.getFile(), effect, output.getFile(), null, progress);
            } else {
                process(input.getFile(), effect, output.getStream(), null, progress);
            }
        } else {
            if (output.isFile()) {
                process(null, input.getStream(), effect, output.getFile(), progress);
            } else {
                process(null, input.getStream(), effect, output.getStream(), null, progress);
            }
        }
    }

}
