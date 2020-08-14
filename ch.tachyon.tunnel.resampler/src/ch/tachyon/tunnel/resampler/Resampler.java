package ch.tachyon.tunnel.resampler;

import java.io.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.resample.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.tools.*;

/**
 * Utility to resample (change the playback speed, affecting both pitch and tempo) an audio file.
 * <p>
 * The implementation loads the {@link Resample} plugin statically, and apply it to the audio stream
 * using the {@link StreamProcessor} utility class.
 * <p>
 * The {@link AudioFileIO} and other utility classes from the <tt>ch.tachyon.tunnel.audio</tt> package
 * are used to decode an audio file into a 32 bit floating point (<tt>float[][]</tt>) PCM audio stream.
 * <p>
 * Usage:
 * <pre>
 * java -jar Resampler.jar <i>input_file</i> <i>ratio</i> <i>output_file</i>
 * </pre>
 * The <tt><i>ratio</i></tt> is the speed change. 1.0 means no change, 2.0 means two times faster,
 * 0.5 means two times slower, etc.
 * @author JUILLERAT
 *
 */
public class Resampler {

    private final static int RESAMPLE_QUALITY = 3;


    /**
     * Main method, implemented using the low-level methods.
     * <p>
     * See {@link #main2(String[])} for a simpler way using the high-level, but more limited {@link SimpleProcessor} class.
     */
    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length != 3) {
            System.out.println("Usage: java -jar Resampler.jar <input file> <ratio> <output file>");
            System.exit(1);
        }

        /*
         * Read arguments from the command line. There must be 3 arguments:
         * - input file
         * - resample ratio, as a floating point number
         * - output file
         */
        File inputFile = new File(args[0]);
        double ratio = Double.parseDouble(args[1]);
        File outputFile = new File(args[2]);

        /*
         * Retrieve the input file format. It contains various informations, such as sample rate, that some
         * plugins might require for optimal results. It will be passed to the StreamProcessor class.
         *
         * Some format (such as .mp3), do not explicitely provide the exact length of the audio in frames.
         * The second argument of the getAudioFileFormat() method is set to true to force getting the exact
         * length if not specified explicitely. This will be done by fully decoding the whole stream if necessary.
         *
         * The exact length *might* be necessary:
         * - By some plugins, such as a "fade out" plugin that needs to know the length of the file
         * - By a progress monitor (IProgressMonitor) passed to AudioFileIO.saveAudioFile() to properly monitor progress
         * - When saving to an *OutputStream* in a format that does not support unknown length (e.g. WAVE, AIFF)
         *   (when saving to a *File* (but not to a FileOutputStream), the length is automatically computed at the end,
         *    and updated by seeking backward in the file)
         *
         * If none of the above applies, then having the exact length is not necessary, and <tt>false</tt> can be used.
         * Requesting the length will then return AudioSystem.NOT_SPECIFIED (-1) if not available.
         */
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFile, true);

        /*
         * Create an IMultiChanAudioSource from the input file.
         *
         * An IMultiChanAudioSource is similar to an InputStream, but it allows you to
         * read decoded PCM audio samples directly as float[][]. The returned instance
         * implements Closeable and can be closed by IOUtils.close() if necessary.
         *
         * All the decoding (wav, mp3) and format conversion (8bit, 16bit, big endian, little endian, etc) is done by AudioFileIO.
         */
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFile);

        /*
         * Create a StreamProcessor to transform the audio using a plugin.
         * Specify the format using the file format of our source file.
         */
        StreamProcessor processor = new StreamProcessor(inputFormat);

        /*
         * Load the resampler plugin. Note that StreamProcessor only works with the IPullEffect interface.
         * However it does not matter if the plugin does not implement this interface, as the tachyon tunnel
         * engine will do the bridge automatically. In fact, the engine will also automatically duplicate
         * the plugin in order to process the two channels of a stereo file, as the plugin itself only handles
         * mono audio streams. This also works for more than two channels.
         */
        IPullEffect plugin = processor.getPlugin(Resample.class);

        /*
         * Set the plugin's parameters. This must be done before the plugin is used
         * using StreamProcessor.filter().
         *
         * This is the only place we can use the plugin directly, and this is limited to public static final fields.
         * A plugin (class Resample in that example) should never be used directly in an other ways.
         *
         * Note that we might get IllegalArgumentException if the specified value is out of the range defined
         * by the plugin.
         */
        plugin.setParameterValue(Resample.RATIO, ratio);
        plugin.setParameterValue(Resample.QUALITY, RESAMPLE_QUALITY);

        /*
         * Now process the audio. This is done by creating a IMultiChanAudioSource that is a filtered
         * version of the original one.
         *
         * Observe that no audio is transformed here. It will be transformed on demand when reading
         * from the returned IMultiChanAudioSource object.
         *
         * Here we just complete the "pipeline" by exposing the source transformed by the effect as a new transformed source
         */
        IMultiChanAudioSource transformedSource = processor.filter(fileSource, plugin);

        /*
         * Choose an output format. In general only PCM formats are supported by Java by default.
         * We use
         * - The same sample rate of the input audio file
         * - 16 bits
         * - Same number of channels as input file
         * - Length is computed from the StreamProcessor object. It will be the same as the input length if no transformation is applied,
         * or if the transformation(s) do not change the audio length. In our case, resampling does change it and this is automatically reflected
         * by the processor's getOutputLength() method.
         *
         * Note that it is valid to pass AudioSystem.NOT_SPECIFIED if you do not know the length. However in that case you cannot save to
         * an OutputStream, you can only save to a File (or you must use the AU file format which supports unspecified length).
         */
        AudioFileFormat format = new AudioFileFormat(AudioFileFormat.Type.WAVE,
                new AudioFormat(inputFormat.getFormat().getSampleRate(), 16, inputFormat.getFormat().getChannels(), true, true),
                (int) processor.getOutputLength());

        /*
         * Save the resulting audio. All the processing takes place here!
         * This can be pretty long, which is why you can supply an IProgressMonitor.
         * Note that the IProgressMonitor can also be used to interrupt the processing (useful if you have a GUI with a 'stop' button)!
         * To do that just throws an OperationAbortedException in setProgress() method of your IProgressMonitor implementation.
         *
         * Oh, and yeah, processing is done using all the available cores, although we did nothing explicitely for it !!!
         * 
         * As an alternative to saving the result to an audio file, you can also manually read from 'transformedSource'
         * and do whatever you want with the resulting sample frames. See the IMultiChanAudioSource class for more information.
         * In that case, use IOUtils.close(transformedSource) at the end of the processing.
         * 
         * Latency, if any, is automatically compensated for. You can safely assume zero latency. Note however that reading N sample frames
         * from the IMultiChanAudioSource might result in much more sample frames to be processed by the plugin.
         */
        AudioFileIO.saveAudioFile(transformedSource, outputFile, format, null);

        /*
         * That's all. The plugin is automatically released when the last audio chunk is read from the source file.
         * The input streams are also automatically closed.
         */
    }
    
    /**
     * Simpler version, using {@link SimpleProcessor}.
     * <p>
     * The simpler {@link SimpleProcessor} class, unlike {@link StreamProcessor}, does not allow you to:
     * <ul>
     * <li>Specify you own audio source by implementing {@link IMultiChanAudioSource} directly
     * <li>Capture and/or modify the audio source by wrapping {@link IMultiChanAudioSource}
     * <li>Chain multiple effects efficiently
     * </ul>
     */
    public static void main2(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length != 3) {
            System.out.println("Usage: java -jar Resampler.jar <input file> <ratio> <output file>");
            System.exit(1);
        }

        /*
         * Read arguments from the command line. There must be 3 arguments:
         * - input file
         * - resample ratio, as a floating point number
         * - output file
         */
        File inputFile = new File(args[0]);
        double ratio = Double.parseDouble(args[1]);
        File outputFile = new File(args[2]);
        
        /*
         * Load the resampler plugin. Note that SimpleProcessor only works with the IPullEffect interface.
         * However it does not matter if the plugin does not implement this interface, as the tachyon tunnel
         * engine will do the bridge automatically. In fact, the engine will also automatically duplicate
         * the plugin in order to process the two channels of a stereo file, as the plugin itself only handles
         * mono audio streams. This also works for more than two channels.
         */
        IPullEffect plugin = SimpleProcessor.getPlugin(Resample.class);
        
        /*
         * Set the plugin's parameters. This must be done before the plugin is used
         * using SimpleProcessor.process().
         *
         * This is the only place we can use the plugin directly, and this is limited to public static final fields.
         * A plugin (class Resample in that example) should never be used directly in an other ways.
         *
         * Note that we might get IllegalArgumentException if the specified value is out of the range defined
         * by the plugin. Parameters can also be specifed by their name, as a String.
         */
        plugin.setParameterValue(Resample.RATIO, ratio); // Resample.RATIO could be replaced by "ratio" (the actual internal name of the parameter)
        plugin.setParameterValue(Resample.QUALITY, RESAMPLE_QUALITY); // Resample.QUALITY could be replaced by "accuracy" (the actual internal name of the parameter)
        
        /*
         * Transform the given audio input file using the plugin, writing the result to the given output file.
         * There are also overloaded version of the process() method using InputStream and OutputStream
         */
        SimpleProcessor.process(inputFile, plugin, outputFile);
    }

}
