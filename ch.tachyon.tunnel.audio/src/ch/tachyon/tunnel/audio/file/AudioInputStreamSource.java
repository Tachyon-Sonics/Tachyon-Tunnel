package ch.tachyon.tunnel.audio.file;

import java.io.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;

import org.tritonus.share.sampled.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.utils.*;

/**
 * Convert an {@link AudioInputStream} of any supported format to a
 * {@link IMultiChannelAudioSource} for reading as floats.
 * <p>
 * This class is typically used to read an audio file, and access its content
 * as float arrays.
 * <p>
 * If the entire stream is read (until {@link IMultiChanAudioSource#readSamples(float[][])} returns less than the
 * passed array size), the underlying stream will be closed. Else it will only be closed
 * once this object is finalized (hence you may need to close it manually earlier).
 */
class AudioInputStreamSource implements IMultiChanAudioSource, Closeable {

    private final AudioInputStream stream;
    private final AudioFormat format;
    private final int nbChannels;
    private final boolean bigEndian;
    private final int bitsPerSample;
    private final boolean unsigned;
    private final int scale;
    private final AudioEncodingType encoding;
    private byte[] buffer;
    private float[] samples;
    private boolean closed = false;


    public AudioInputStreamSource(AudioInputStream stream) {
        stream = convertToLinear(stream);
        this.stream = stream;
        this.format = stream.getFormat();
        this.nbChannels = format.getChannels();
        if (nbChannels == AudioSystem.NOT_SPECIFIED)
            throw new IllegalArgumentException("Number of channels is not specified");
        this.bigEndian = format.isBigEndian();
        if (format.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED)
            bitsPerSample = 16;
        else
            bitsPerSample = format.getSampleSizeInBits();
        if (format.getEncoding().equals(Encoding.PCM_SIGNED))
            this.unsigned = false;
        else if (format.getEncoding().equals(Encoding.PCM_UNSIGNED))
            this.unsigned = true;
        else if (format.getEncoding().equals(Encodings.PCM_IEEE_FLOAT))
            this.unsigned = false;
        else
            throw new IllegalArgumentException("Unsupported encoding: " + format.getEncoding());
        if (unsigned && bitsPerSample != 8)
            throw new IllegalArgumentException("Unsigned format not supported for bits per sample of " + bitsPerSample);
        this.scale = (bitsPerSample + 7) / 8;
        if (format.getEncoding().equals(Encodings.PCM_IEEE_FLOAT)) {
            if (bitsPerSample == 64)
                this.encoding = (bigEndian ? AudioEncodingType.ENC_DOUBLE_BE : AudioEncodingType.ENC_DOUBLE_LE);
            else
                this.encoding = (bigEndian ? AudioEncodingType.ENC_FLOAT_BE : AudioEncodingType.ENC_FLOAT_LE);
        } else {
            if (bitsPerSample == 8)
                this.encoding = (unsigned ? AudioEncodingType.ENC_8BIT_U : AudioEncodingType.ENC_8BIT_S);
            else if (bitsPerSample == 16)
                this.encoding = (bigEndian ? AudioEncodingType.ENC_16BIT_BE : AudioEncodingType.ENC_16BIT_LE);
            else if (bitsPerSample == 24)
                this.encoding = (bigEndian ? AudioEncodingType.ENC_24BIT_BE : AudioEncodingType.ENC_24BIT_LE);
            else if (bitsPerSample == 32)
                this.encoding = (bigEndian ? AudioEncodingType.ENC_32BIT_BE : AudioEncodingType.ENC_32BIT_LE);
            else
                throw new IllegalArgumentException("Unsupported bits per sample: " + bitsPerSample);
        }
    }

    /**
     * Convert the audio input stream to a linear format
     */
    private static AudioInputStream convertToLinear(AudioInputStream stream) {
        AudioFormat sourceFormat = stream.getFormat();
        Encoding encoding = Encoding.PCM_SIGNED;
        if (sourceFormat.getSampleSizeInBits() == 8 && sourceFormat.getEncoding().equals(Encoding.PCM_UNSIGNED))
            encoding = Encoding.PCM_UNSIGNED;
        else if (sourceFormat.getEncoding().equals(Encodings.PCM_IEEE_FLOAT))
            encoding = Encodings.PCM_IEEE_FLOAT;
        int sampleSizeInBits = sourceFormat.getSampleSizeInBits();
        if (sampleSizeInBits == AudioSystem.NOT_SPECIFIED)
            sampleSizeInBits = 16;
        AudioFormat targetFormat = new AudioFormat(
                encoding,
                sourceFormat.getSampleRate(),
                sampleSizeInBits,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * sampleSizeInBits / 8,
                sourceFormat.getSampleRate(),
                sourceFormat.isBigEndian());
        AudioInputStream result = AudioSystem.getAudioInputStream(targetFormat, stream);
        return result;
    }

    @Override
    public AudioFormat getAudioFormat() {
        return new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), true, false);
    }

    @Override
    public int readSamples(float[][] target) {
        if (target.length != nbChannels)
            throw new IllegalArgumentException("Number fo channels does not match");
        int chanLength = target[0].length;
        int fullLength = chanLength * nbChannels;
        int bLength = fullLength * scale;
        if (buffer == null || buffer.length < bLength)
            buffer = new byte[bLength];
        if (samples == null || samples.length < fullLength)
            samples = new float[fullLength];
        try {
            int amount = 0;
            while (amount < bLength) {
                if (closed)
                    break;
                int read = stream.read(buffer, amount, bLength - amount);
                if (read < 0) {
                    closed = true;
                    stream.close();
                    break;
                }
                amount += read;
            }
            assert amount % (scale * nbChannels) == 0;
            fullLength = amount / scale;
            chanLength = fullLength / nbChannels;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (OperationAbortedException ex) {
            if (!closed) {
                closed = true;
                IOUtils.close(stream);
            }
            throw ex;
        }

        encoding.decode(buffer, samples, fullLength);
        decodeChannels(samples, target, chanLength);
        return chanLength;
    }

    private void decodeChannels(float[] samples, float[][] target, int chanLength) {
        if (nbChannels == 1) {
            System.arraycopy(samples, 0, target[0], 0, chanLength);
        } else {
            for (int chan = 0; chan < nbChannels; chan++) {
                for (int i = 0; i < chanLength; i++)
                    target[chan][i] = samples[i * nbChannels + chan];
            }
        }
    }

    public int getNbChannels() {
        return this.nbChannels;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            IOUtils.close(stream);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

}
