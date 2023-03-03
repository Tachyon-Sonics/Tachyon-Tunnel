package ch.tachyon.opus.spi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import org.concentus.OpusException;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;
import org.gagravarr.opus.OpusTags;


public class OpusAudioFileReader extends AudioFileReader {
    
    private final static int READ_LIMIT = 10000;

    
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        if (!stream.markSupported())
            stream = new BufferedInputStream(stream, READ_LIMIT);
        stream.mark(READ_LIMIT);
        try {
            return getAudioFileFormatImpl(stream, false);
        } finally {
            stream.reset();
        }
    }
    
    private AudioFileFormat getAudioFileFormatImpl(InputStream stream, boolean calculateLength) throws UnsupportedAudioFileException {
        try {
            // Create audio format
            OggFile oggFile = new OggFile(stream);
            OpusFile opusFile = new OpusFile(oggFile);
            OpusInfo opusInfo = opusFile.getInfo();
            AudioFormat format = new AudioFormat(
                    Opus.OPUS_ENCODING, 
                    opusInfo.getSampleRate(), 
                    16, 
                    opusInfo.getNumChannels(), 
                    AudioSystem.NOT_SPECIFIED, 
                    AudioSystem.NOT_SPECIFIED, 
                    false);
            
            // Gather properties
            Map<String, Object> properties = new HashMap<>();
            properties.put("outputGain", opusInfo.getOutputGain());
            OpusTags tags = opusFile.getTags();
            if (tags != null) {
                Map<String, List<String>> comments = tags.getAllComments();
                for (Map.Entry<String, List<String>> entry : comments.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if (values.size() == 1)
                        properties.put(key, values.get(0));
                    else if (values.size() > 1)
                        properties.put(key, values);
                }
            }
            
            // Create audio file format
            long frameLength = AudioSystem.NOT_SPECIFIED;
            if (stream instanceof FileInputStream && calculateLength) {
                // Calculate length
                frameLength = calculateFrameLength(opusFile, opusInfo);
                properties.put(Opus.FRAME_LENGTH_PROPERTY, frameLength);
            }
            AudioFileFormat fileFormat;
            fileFormat = new AudioFileFormat(Opus.OPUS_FILE_TYPE, format, cropLength(frameLength), properties);
            return fileFormat;
        } catch (Exception ex) {
            throw new UnsupportedAudioFileException(ex.getMessage());
        }
    }

    private long calculateFrameLength(OpusFile opusFile, OpusInfo opusInfo) throws IOException, OpusException {
        long totalFrames = 0;
        while (true) {
            OpusAudioData audioData = opusFile.getNextAudioPacket();
            if (audioData == null)
                break;
            int nbSamples = audioData.getNumberOfSamples();
            totalFrames += nbSamples;
        }
        return totalFrames - opusInfo.getPreSkip();
        
        /*
         * The version below is not significantly faster, and is not consistent with decoding. Not sure why...
         */
//        OpusStatistics stats = new OpusStatistics(opusFile);
//        stats.calculate();
//        double durationSeconds = stats.getDurationSeconds();
//        long frameLength = (long) (durationSeconds * (double) opusInfo.getSampleRate() + 0.5);
//        return frameLength;
    }
    
    private int cropLength(long length) {
        if (length > Integer.MAX_VALUE)
            return AudioSystem.NOT_SPECIFIED;
        return (int) length;
    }
    
    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream input = url.openStream();
        AudioFileFormat result = getAudioFileFormatImpl(input, false);
        input.close();
        return result;
    }

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        FileInputStream input = new FileInputStream(file);
        AudioFileFormat result = getAudioFileFormatImpl(input, true);
        input.close();
        return result;
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        if (!stream.markSupported())
            stream = new BufferedInputStream(stream, READ_LIMIT);
        AudioFileFormat fileFormat = getAudioFileFormat(stream);
        return new AudioInputStream(stream, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(url.openStream());
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(new FileInputStream(file));
    }

}
