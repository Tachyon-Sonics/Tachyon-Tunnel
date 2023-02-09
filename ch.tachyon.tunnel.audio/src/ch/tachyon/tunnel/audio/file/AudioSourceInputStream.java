package ch.tachyon.tunnel.audio.file;

import java.io.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;

import org.tritonus.share.sampled.*;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;

/**
 * Convert a {@link IMultiChanAudioSource} (audio data as floats) into an
 * {@link InputStream} with the given {@link AudioEncodingType}.
 * <p>
 * {@link #createAudioInputStream(IMultiChanAudioSource, AudioFormat, IProgressMonitor, long)} internally
 * uses this class to directly convert an {@link IMultiChanAudioSource} to an
 * {@link AudioInputStream}. This method is typically used when saving to file an audio file stored as float arrays in memory.
 * <p>
 * Optionally report reading progress to an {@link IProgressMonitor}. If the progress monitor
 * throws an {@link InterruptedException}, an {@link IOException} with message <code>Interrupted</code>
 * is thrown.
 */
public class AudioSourceInputStream extends InputStream {

    private final static int BUFFER_SIZE = 32768; // in frames

    private final IMultiChanAudioSource source;
    private final AudioEncodingType encoding;
    private final float[][] samples;
    private final float[] channels; // With interleaved channels
    private final byte[] buffer;
    private final int nbChannels;
    private int bufferLength;
    private int bufferOffset;

    private final IProgressMonitor progress;
    private final long progressLength;
    private long progressOffset;
    private OperationAbortedException abortion;


    /**
     * @param source the source
     * @param encoding the encoding to use for the stream
     * @param progress optional progress monitor
     * @param length the length of the audio in frames (used if a progress monitor is specified)
     */
    public AudioSourceInputStream(IMultiChanAudioSource source, int nbChannels, AudioEncodingType encoding, IProgressMonitor progress, long length) {
        this.source = source;
        this.encoding = encoding;
        this.nbChannels = nbChannels;
        this.samples = new float[nbChannels][BUFFER_SIZE];
        this.channels = new float[BUFFER_SIZE * nbChannels];
        this.buffer = new byte[BUFFER_SIZE * encoding.getBytesPerSample() * nbChannels];
        bufferLength = buffer.length;
        bufferOffset = buffer.length;
        this.progress = progress;
        this.progressLength = length;
    }
    
    @Override
    public int read() throws IOException {
        if (bufferLength == 0)
            return -1;
        if (bufferOffset >= bufferLength) {
            fillNextBuffer();
            if (bufferLength == 0)
                return -1;
        }
        return (int) buffer[bufferOffset++] & 0xFF;
    }

    private void fillNextBuffer() throws IOException {
        int nbFrames;
        if (abortion != null)
            nbFrames = 0;
        else
            nbFrames = source.readSamples(samples);
        if (nbFrames < BUFFER_SIZE) {
            // Pad with zero (although these values should never get read)
            for (int chan = 0; chan < nbChannels; chan++)
                Arrays.fill(samples[chan], nbFrames, BUFFER_SIZE, 0.0f);
            // Adjust byte buffer length to number of samples read
            bufferLength = nbFrames * encoding.getBytesPerSample() * nbChannels;
        }
        bufferOffset = 0;
        encodeChannels(samples, channels, nbFrames);
        encoding.encode(channels, buffer, nbFrames * nbChannels);
        if (progress != null && abortion == null) {
            try {
                progress.setProgress(progressOffset, progressLength);
                progressOffset += nbFrames;
            } catch (OperationAbortedException ex) {
                abortion = ex;
            }
        }
    }

    private void encodeChannels(float[][] source, float[] target, int nbFrames) {
        if (nbChannels == 1) {
            System.arraycopy(source[0], 0, target, 0, nbFrames);
        } else {
            for (int chan = 0; chan < nbChannels; chan++) {
                for (int i = 0; i < nbFrames; i++)
                    target[i * nbChannels + chan] = source[chan][i];
            }
        }
    }
    
    public OperationAbortedException aborted() {
        OperationAbortedException result = abortion;
        abortion = null;
        return result;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (progress != null && abortion == null) {
            try {
                progress.setProgress(progressLength, progressLength);
            } catch (OperationAbortedException ex) {
                abortion = ex;
            }
        }
    }

    /**
     * Create an {@link AudioInputStream} that reads from the given {@link IMultiChanAudioSource} and
     * encodes in the given format.
     * @param progress progress monitor (optional)
     * @param length the length in frames, or {@link AudioSystem#NOT_SPECIFIED}
     */
    public static AudioInputStream createAudioInputStream(IMultiChanAudioSource source, AudioFormat format, IProgressMonitor progress, long length) {
        AudioSourceInputStream stream = createAudioSourceInputStream(source, format, progress, length);
        return new AudioInputStream(stream, format, length);
    }

    public static AudioSourceInputStream createAudioSourceInputStream(IMultiChanAudioSource source, AudioFormat format, IProgressMonitor progress, long length) {
        AudioEncodingType encoding = null;
        int bits = format.getSampleSizeInBits();
        boolean bigEndian = format.isBigEndian();
        if (format.getEncoding().equals(Encoding.PCM_SIGNED)) {
            if (bits == 8)
                encoding = AudioEncodingType.ENC_8BIT_S;
            else if (bits == 16)
                encoding = (bigEndian ? AudioEncodingType.ENC_16BIT_BE : AudioEncodingType.ENC_16BIT_LE);
            else if (bits == 24)
                encoding = (bigEndian ? AudioEncodingType.ENC_24BIT_BE : AudioEncodingType.ENC_24BIT_LE);
            else if (bits == 32)
                encoding = (bigEndian ? AudioEncodingType.ENC_32BIT_BE : AudioEncodingType.ENC_32BIT_LE);
        } else if (format.getEncoding().equals(Encoding.PCM_UNSIGNED)) {
            if (bits == 8)
                encoding = AudioEncodingType.ENC_8BIT_U;
        } else if (format.getEncoding().equals(Encodings.PCM_IEEE_FLOAT)) {
            if (bits == 64)
                encoding = (bigEndian ? AudioEncodingType.ENC_DOUBLE_BE : AudioEncodingType.ENC_DOUBLE_LE);
            else
                encoding = (bigEndian ? AudioEncodingType.ENC_FLOAT_BE : AudioEncodingType.ENC_FLOAT_LE);
        }
        if (encoding == null)
            throw new UnsupportedOperationException("Unsupported audio format. Only PCM_SIGNED 8/16/24/32bit, PCM_UNSIGNED 8bit and PCM_FLOAT are supported");
        AudioSourceInputStream stream = new AudioSourceInputStream(source, format.getChannels(), encoding, progress, length);
        return stream;
    }

}
