package ch.tachyon.tunnel.audio.file;

import java.util.*;

import javax.sound.sampled.*;


public class AudioFileFormatEx extends AudioFileFormat {
    
    private long frameLength;
    

    public AudioFileFormatEx(Type type, AudioFormat format, long frameLength, Map<String, Object> properties) {
        super(type, format, truncate(frameLength), properties);
        this.frameLength = frameLength;
    }

    public AudioFileFormatEx(Type type, AudioFormat format, long frameLength) {
        super(type, format, truncate(frameLength));
        this.frameLength = frameLength;
    }

    public AudioFileFormatEx(Type type, int byteLength, AudioFormat format, long frameLength) {
        super(type, byteLength, format, truncate(frameLength));
        this.frameLength = frameLength;
    }
    
    public long getFrameLengthLong() {
        return frameLength;
    }
    
    public void setFrameLength(long frameLength) {
        this.frameLength = frameLength;
    }

    private static int truncate(long value) {
        if (value > 0x7FFFFFFF)
            return AudioSystem.NOT_SPECIFIED;
        return (int) value;
    }

}
