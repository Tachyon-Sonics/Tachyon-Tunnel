package ch.tachyon.tunnel.resampler;

import java.io.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.demo.plugin.timestretch.*;
import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.spec.*;

/**
 * Same as {@link TimeStretcher}, but the plugin is used directly, without passing through the ch.tachyon.tunnel.host package,
 * and hence without using the ch.tachyon.tunnel.engine package.
 * <p>
 * This reduces the dependences, as only the ch.tachyon.tunnel.plugin and ch.tachyon.tunnel.common packages are used. This class
 * also uses the ch.tachyon.tunnel.audio.file package to simplify audio file reading and writing.
 * <p>
 * On the other hand, the plugin usage is much more complex and error-prone, and while the below code works for a given plugin
 * ({@link GradientTimeStretch}), it may fail with another, as many things are actually dependent on the plugin's implementation,
 * such as:
 * <ul>
 * <li>We have to provide an {@link IProcessingInfo} implementation to the plugin. Here we use an instance of class
 * {@link SimpleProcessingInfo}, which can be constructed from an {@link AudioFileFormat}. Note that only
 * {@link SimpleProcessingInfo#getNumberOfChannels()} and {@link SimpleProcessingInfo#getSampleRate()} are actually used by the 
 * {@link GradientTimeStretch} plugin.
 * <li>Because the plugin implements {@link IStartStop}, we call its {@link GradientTimeStretch#startProcessing(IProcessingInfo)}
 * and {@link GradientTimeStretch#stopProcessing()} methods. Other plugins might implement other interfaces with other methods that
 * need to be invoked at the right time.
 * <li>Because the plugin implements {@link IMultiChanPullEffect}, processing must be done using
 * {@link IMultiChanPullEffect#process(IMultiChanAudioSource, float[][])}.
 * <li>Because the plugin implements {@link IFixedChunkLength}, we can only produce audio chunks of a fixed size, given by the
 * result of this method. This method must be called after the parameters are set, and after
 * {@link GradientTimeStretch#startProcessing(IProcessingInfo)}.
 * <li>There is no automatic latency compensation. This implementation does not implement manual latency compensation either.
 * You may do so by requesting the latency using {@link GradientTimeStretch#getLatency(IoDirection)}, after
 * {@link GradientTimeStretch#startProcessing(IProcessingInfo)}.
 * </ul>
 */
public class TimeStretcherAlt {
    
    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length != 3) {
            System.out.println("Usage: java -jar TimeStretcherAlt.jar <input file> <ratio> <output file>");
            System.exit(1);
        }

        // Parse arguments
        File inputFile = new File(args[0]);
        double ratio = Double.parseDouble(args[1]);
        File outputFile = new File(args[2]);
        
        // Get input format and create output format
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFile, true);
        AudioFileFormat outputFormat = new AudioFileFormat(AudioFileFormat.Type.WAVE,
                new AudioFormat(inputFormat.getFormat().getSampleRate(), 16, inputFormat.getFormat().getChannels(), true, true),
                (int) (inputFormat.getFrameLength() * ratio + 0.5));
        
        // Create an audio source from the input file
        IMultiChanAudioSource fileSource = AudioFileIO.createAudioSource(inputFile);
        
        // Create a minimal processing info, with number of channels and sample rate, based on the input file format
        IProcessingInfo processingInfo = new SimpleProcessingInfo(inputFormat);

        // Instanciate the time-stretching plugin, set the parameters and prepare for processing
        GradientTimeStretch stretcher = new GradientTimeStretch();
        stretcher.setRatio(ratio);
        stretcher.setZeroPad(false);
        
        stretcher.startProcessing(processingInfo); // FFT engine and buffers are created here
        int chunkLength = stretcher.getFixedChunkLength();
        int nbChans = inputFormat.getFormat().getChannels();
        float[][] inputFrames = new float[nbChans][chunkLength];
        
        // Create an output audio sink
        OutputStream output = new FileOutputStream(outputFile);
        OutputStreamSaveAudioSink fileSink = new OutputStreamSaveAudioSink(output, outputFormat, null);
        
        // Process using the IMultiChanPullEffect interface
        while (true) {
            // Generate next chunk of output audio frames
            int nbFrames = stretcher.process(fileSource, inputFrames);
            if (nbFrames <= 0)
                break; // End of file
            
            // Write the chunk of output frames
            float[][] outputFrames = inputFrames;
            if (nbFrames < chunkLength) { // Last chunk can be shorter
                outputFrames = new float[nbChans][nbFrames];
                for (int chan = 0; chan < nbChans; chan++)
                    System.arraycopy(inputFrames[chan], 0, outputFrames[chan], 0, nbFrames);
            }
            fileSink.writeSamples(outputFrames);
        }
        
        // Release ressources and close files
        stretcher.stopProcessing();
        fileSink.close();
        fileSource.close();
    }

}
