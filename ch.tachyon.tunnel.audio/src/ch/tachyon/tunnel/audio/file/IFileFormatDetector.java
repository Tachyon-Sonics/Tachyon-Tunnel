package ch.tachyon.tunnel.audio.file;

import java.io.*;

import javax.sound.sampled.*;

public interface IFileFormatDetector {
    
    /**
     * Improve format detection using the given file
     * @param format the format detected using an Input Stream
     * @return an improved format (with less unspecifed fields), or the given format if nothing can be improved
     */
    public AudioFormat detectFormat(File file, AudioFormat format) throws IOException, UnsupportedAudioFileException;
    
    /**
     * Detect file length
     * @param format detected format with unknown length
     * @return the detected length, or -1 if it cannot be detected
     */
    public long detectLength(File file, AudioFileFormat format) throws IOException, UnsupportedAudioFileException;

}
