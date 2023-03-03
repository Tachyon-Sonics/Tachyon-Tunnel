package ch.tachyon.opus.spi;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/**
 * Encoding and file type values for the opus format
 */
public class Opus {
    
    /**
     * Property key (for {@link AudioFileFormat#getProperty(String)} whose value is a Long
     * corresponding to the duration of the file in sample frames.
     * <p>
     * Please note that the duration is only calculated when {@link AudioSystem#getAudioFileFormat(java.io.File)}
     * with a {@link File} argument is used.
     */
    public static final String FRAME_LENGTH_PROPERTY = "frameLength";
    
    public static final AudioFormat.Encoding OPUS_ENCODING = new AudioFormat.Encoding("OPUS");
    
    public final static AudioFileFormat.Type OPUS_FILE_TYPE = new AudioFileFormat.Type("OPUS", "opus");

}
