package ch.tachyon.tunnel.demo.host;

import java.io.*;
import javax.sound.sampled.*;
import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;
import ch.tachyon.tunnel.host.engine.*;
import ch.tachyon.tunnel.utils.*;

/**
 * Holds the logic of applying an audio effect to an input file
 */
public class ApplyPlugin {
    
    final static int CHUNK_LENGTH = 4096;
    
    
    public static void applyPlugin(File inputFile, IPluginBundle bundle, String effectClassName, byte[] parameters, File outputFile, IProgressMonitor progress) throws IOException, UnsupportedAudioFileException {
        /*
         * Retrieve the input file format. We need the sample rate, the length in sample frames, and the number of channels
         */
        AudioFileFormat inputFormat = AudioFileTypes.instance().getAudioFileFormat(inputFile, true);
        float sampleRate = inputFormat.getFormat().getSampleRate();
        int inputLength = inputFormat.getFrameLength();
        int nbChannels = inputFormat.getFormat().getChannels();
        
        /*
         * Create an IMultiChanAudioSource from the input file.
         * 
         * An IMultiChanAudioSource is similar to an InputStream, but it allows you to
         * read decoded PCM audio samples directly as float[][]. The returned instance
         * implements Closeable and can be closed by IOUtils.close() if necessary.
         * 
         * All the decoding (wav, mp3) and format conversion (8bit, 16bit, big endian, little endian, etc) is done by AudioFileIO.
         */
        IMultiChanAudioSource audioSource = AudioFileIO.createAudioSource(inputFile, progress);
        
        IProcessingInfo info = new ProcessingInfo(nbChannels, sampleRate, 16, inputLength, CHUNK_LENGTH, CHUNK_LENGTH);
        
        final IPushEffect plugin = bundle.loadPlugin(IPushEffect.class, effectClassName);
        plugin.load(info);
        
        // Set parameters
        try {
            Storage.loadParameterValues(new ByteArrayInputStream(parameters), bundle, plugin.getParameters());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        plugin.startProcessing(info);
        
        OutputStream outputStream = new FileOutputStream(outputFile);
        int outputLength = (int) ((float) inputLength * plugin.getTimeChange() + 0.5f);
        AudioFileFormat outputFormat = new AudioFileFormat(AudioFileFormat.Type.WAVE, new AudioFormat(sampleRate, 16, nbChannels, true, false), outputLength);
        OutputStreamSaveAudioSink streamSaveSink = new OutputStreamSaveAudioSink(outputStream, outputFormat, null);
        
        try {
            float[][] buffer = new float[nbChannels][CHUNK_LENGTH];
            int nbFramesAvailable;
            do {
                nbFramesAvailable = audioSource.readSamples(buffer);
                plugin.process(buffer, nbFramesAvailable, streamSaveSink);
            } while (nbFramesAvailable == CHUNK_LENGTH);
        } finally {
            streamSaveSink.close();
            plugin.stopProcessing();
            plugin.unload();
        }
    }

}
