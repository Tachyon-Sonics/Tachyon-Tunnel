package ch.tachyon.tunnel.resampler;

import java.io.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.demo.plugin.timestretch.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.tools.*;

/**
 * Utility to time stretch (change the speed without affecting the pitch) an audio file.
 * <p>
 * The implementation loads the {@link GradientTimeStretch} plugin statically, and apply it to the audio stream
 * using the {@link StreamProcessor} utility class.
 * <p>
 * The {@link AudioFileIO} and other utility classes from the <tt>ch.tachyon.tunnel.audio</tt> package
 * are used to decode an audio file into a 32 bit floating point (<tt>float[][]</tt>) PCM audio stream.
 * <p>
 * Usage:
 * <pre>
 * java -jar TimeStretcher.jar <i>input_file</i> <i>ratio</i> <i>output_file</i>
 * </pre>
 * The <tt><i>ratio</i></tt> is the speed change. 1.0 means no change, 2.0 means two times slower,
 * 0.5 means two times faster, etc.
 * @author JUILLERAT
 *
 */
public class TimeStretcher {

    // See Resampler.java for detailled explanations
    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length != 3 && args.length != 4) {
            System.out.println("Usage: java -jar TimeStretcher.jar [-z] <input file> <ratio> <output file>");
            System.exit(1);
        }

        boolean zeroPad = false;
        if (args.length == 4) {
            if (args[0].equals("-z"))
                zeroPad = true;
            System.arraycopy(args, 1, args, 0, 3);
        }
        
        File inputFile = new File(args[0]);
        double ratio = Double.parseDouble(args[1]);
        File outputFile = new File(args[2]);
        
        IPullEffect plugin = SimpleProcessor.getPlugin(GradientTimeStretch.class);
        
        plugin.setParameterValue("ratio", ratio);
        plugin.setParameterValue("zeroPad", zeroPad);
        
        SimpleProcessor.process(inputFile, plugin, outputFile);
    }

}
