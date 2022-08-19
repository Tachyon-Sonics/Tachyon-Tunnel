package ch.tachyon.tunnel.audio.utils;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;

import ch.tachyon.tunnel.common.*;

/**
 * Conversion between {@link AudioStreamFormat} and {@link AudioFormat}
 */
public class FormatUtils {
    
    public static AudioStreamFormat toStreamFormat(AudioFormat format) {
        int precision = format.getSampleSizeInBits();
        if (precision == AudioSystem.NOT_SPECIFIED)
            precision = 32;
        AudioStreamFormat sFormat = new AudioStreamFormat(format.getSampleRate(), format.getChannels(), precision);
        sFormat.setAudioFormat(format);
        return sFormat;
    }
    
    public static AudioFormat fromStreamFormat(AudioStreamFormat sFormat) {
        if (sFormat.getAudioFormat() instanceof AudioFormat)
            return (AudioFormat) sFormat.getAudioFormat();
        return new AudioFormat(
                Encoding.PCM_FLOAT, 
                sFormat.getSampleRate(), 
                sFormat.getQuantization(), 
                sFormat.getChannels(), 
                (sFormat.getQuantization() + 7) / 8 * sFormat.getChannels(), 
                sFormat.getSampleRate(),
                false);
    }

}
