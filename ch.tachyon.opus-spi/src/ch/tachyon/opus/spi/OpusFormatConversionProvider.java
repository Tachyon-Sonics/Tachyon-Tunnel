package ch.tachyon.opus.spi;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

import org.concentus.OpusException;


public class OpusFormatConversionProvider extends FormatConversionProvider {

    @Override
    public Encoding[] getSourceEncodings() {
        return new Encoding[] { Opus.OPUS_ENCODING };
    }

    @Override
    public Encoding[] getTargetEncodings() {
        return new Encoding[] { Encoding.PCM_SIGNED };
    }

    @Override
    public Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(Opus.OPUS_ENCODING))
            return getTargetEncodings();
        return new Encoding[0];
    }

    @Override
    public AudioFormat[] getTargetFormats(Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(Opus.OPUS_ENCODING)
                && targetEncoding.equals(Encoding.PCM_SIGNED)) {
            return new AudioFormat[] { 
                    decodedFormat(sourceFormat)
            };
        }
        return new AudioFormat[0];
    }
    
    private AudioFormat decodedFormat(AudioFormat encodedFormat) {
        if (!encodedFormat.getEncoding().equals(Opus.OPUS_ENCODING))
            throw new IllegalArgumentException("Format is not in OPUS encoding");
        return new AudioFormat(
                Encoding.PCM_SIGNED,
                encodedFormat.getSampleRate(),
                16,
                encodedFormat.getChannels(),
                2 * encodedFormat.getChannels(),
                encodedFormat.getSampleRate(),
                false);
    }

    @Override
    public AudioInputStream getAudioInputStream(Encoding targetEncoding, AudioInputStream sourceStream) {
        if (!targetEncoding.equals(Encoding.PCM_SIGNED))
            throw new IllegalArgumentException("Target encoding is not PCM_SIGNED");
        try {
            return new DecodedOpusAudioInputStream(sourceStream, decodedFormat(sourceStream.getFormat()), -1);
        } catch (IOException | OpusException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (!targetFormat.getEncoding().equals(Encoding.PCM_SIGNED))
            throw new IllegalArgumentException("Target audio format is not in PCM_SIGNED encoding");
        if (targetFormat.getSampleSizeInBits() != 16)
            throw new IllegalArgumentException("Target audio format is not in 16 bit");
        if (targetFormat.isBigEndian())
            throw new IllegalArgumentException("Target audio format is not little-endian");
        try {
            return new DecodedOpusAudioInputStream(sourceStream, targetFormat, -1);
        } catch (IOException | OpusException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
