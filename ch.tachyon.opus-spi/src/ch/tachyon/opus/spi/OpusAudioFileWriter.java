package ch.tachyon.opus.spi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;

import org.concentus.OpusApplication;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;
import org.concentus.OpusSignal;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;
import org.gagravarr.opus.OpusTags;

/**
 * JavaSound provider implementation for writing audio files in the OPUS format.
 * <p>
 * As of writing, this class is working, but inherits a few remaining bugs from
 * the used libraries, org.concentus and org.gagravarr.opus; namely the length is
 * not detected correctly by other players, and the exact frame length seems to drift
 * a little.
 * <p>
 * You may want to update to the most up-to-date versions of the org.concentus and
 * org.gagravarr.opus libraries.
 */
public class OpusAudioFileWriter extends AudioFileWriter {

    /**
     * Property key in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}.
     * Value must be a {@link OpusApplication}.
     * Default value is {@link OpusApplication#OPUS_APPLICATION_AUDIO}.
     */
    public static final String OPUS_APPLICATION_KEY = "opus.application";

    /**
     * Property key in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}.
     * Value must be a {@link OpusSignal}.
     * Default value is {@link OpusSignal#OPUS_SIGNAL_MUSIC}.
     */
    public static final String OPUS_SIGNAL_KEY = "opus.signal";

    /**
     * Property key in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}.
     * Value must be an {@link Integer}.
     * Default value is <tt>96000</tt>.
     */
    public static final String OPUS_BITRATE_KEY = "opus.bitrate";
    
    /**
     * Property key in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}.
     * Value must be an {@link Integer}.
     * Default value is <tt>960</tt>.
     */
    public static final String OPUS_PACKET_SAMPLES_KEY = "opus.packet.samples";

    /**
     * Property key in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}.
     * Value must be an {@link Integer}.
     * Default value is <tt>10</tt>.
     */
    public static final String OPUS_COMPLEXITY_KEY = "opus.complexity";
    
    /**
     * For any property in {@link AudioFormat#properties()} used by {@link OpusAudioFileWriter}
     * that starts with this prefix, the prefix is removed, and then an opus tag is added
     * with the suffix as tag name, and the value as tag value.
     * Value must be a {@link String}
     * <p>
     * Example:
     * <li>key: <tt>{@link #OPUS_TAG_KEY_PREFIX} + "encoding" // "opustag:encoding" </tt>
     * <li>value: <tt>"ch.tachyon.opus"</tt>
     * <p>
     * Be aware that {@link OpusAudioFileReader#getAudioFileFormat(File)} will return the tags
     * in {@link AudioFileFormat#properties()}, but without the prefix.
     */
    public static final String OPUS_TAG_KEY_PREFIX = "opustag:";
    
    
    @Override
    public Type[] getAudioFileTypes() {
        return new Type[] { Opus.OPUS_FILE_TYPE };
    }

    @Override
    public Type[] getAudioFileTypes(AudioInputStream stream) {
        boolean supported = true;
        AudioFormat sourceFormat = stream.getFormat();
        if (!sourceFormat.getEncoding().equals(Encoding.PCM_SIGNED))
            supported = false;
        if (sourceFormat.getSampleSizeInBits() != 16)
            supported = false;
        if (sourceFormat.isBigEndian())
            supported = false;
        if (supported)
            return getAudioFileTypes();
        return new Type[0];
    }

    @Override
    public int write(AudioInputStream stream, Type fileType, OutputStream output) throws IOException {
        if (!fileType.equals(Opus.OPUS_FILE_TYPE))
            throw new IllegalArgumentException("fileType is not Opus.OPUS_FILE_TYPE");
        AudioFormat sourceFormat = stream.getFormat();
        
        OpusApplication opusApplication = getProperty(sourceFormat, OPUS_APPLICATION_KEY, OpusApplication.class, 
                OpusApplication.OPUS_APPLICATION_AUDIO);
        OpusSignal opusSignal = getProperty(sourceFormat, OPUS_SIGNAL_KEY, OpusSignal.class, 
                OpusSignal.OPUS_SIGNAL_MUSIC);
        int bitRate = getProperty(sourceFormat, OPUS_BITRATE_KEY, Integer.class, 96000);
        int packetSamples = getProperty(sourceFormat, OPUS_PACKET_SAMPLES_KEY, Integer.class, 960);
        int complexity = getProperty(sourceFormat, OPUS_COMPLEXITY_KEY, Integer.class, 10);
        Integer preSkip = getProperty(sourceFormat, Opus.OPUS_PRE_SKIP_KEY, Integer.class, null);
        Integer outputGain = getProperty(sourceFormat, Opus.OPUS_OUTPUT_GAIN_KEY, Integer.class, null);
        
        OpusEncoder encoder;
        try {
            encoder = new OpusEncoder(
                    (int) sourceFormat.getSampleRate(), 
                    sourceFormat.getChannels(), 
                    opusApplication);
        } catch (OpusException ex) {
            throw new IOException(ex);
        }
        encoder.setBitrate(bitRate);
        encoder.setSignalType(opusSignal);
        encoder.setComplexity(complexity);
        
        OpusInfo info = new OpusInfo();
        info.setNumChannels(sourceFormat.getChannels());
        info.setSampleRate((long) sourceFormat.getSampleRate());
        if (preSkip != null)
            info.setPreSkip(preSkip);
        if (outputGain != null)
            info.setOutputGain(outputGain);
        OpusTags tags = new OpusTags();
        for (Map.Entry<String, Object> entry : sourceFormat.properties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(OPUS_TAG_KEY_PREFIX) && (value instanceof String)) {
                String tagName = key.substring(OPUS_TAG_KEY_PREFIX.length());
                tags.addComment(tagName, (String) value);
            }
        }
        
        OpusFile file = new OpusFile(output, info, tags);
        byte[] inBuf = new byte[packetSamples * 2 * 2];
        byte[] data_packet = new byte[1275];
        int result = 0;
        try {
            while (stream.available() >= inBuf.length) {
                int bytesRead = stream.read(inBuf, 0, inBuf.length);
                if (bytesRead < inBuf.length)
                    Arrays.fill(inBuf, bytesRead, inBuf.length, (byte) 0); // Pad with silence
                short[] pcm = bytesToShorts(inBuf, 0, inBuf.length);
                int bytesEncoded = encoder.encode(pcm, 0, bytesRead / 4, data_packet, 0, 1275);
                result += bytesEncoded;
                byte[] packet = new byte[bytesEncoded];
                System.arraycopy(data_packet, 0, packet, 0, bytesEncoded);
                OpusAudioData data = new OpusAudioData(packet);
                file.writeAudioData(data);
                result += data.getOggOverheadSize();
            }
        } catch (OpusException ex) {
            throw new IOException(ex);
        } finally {
            file.close();
            output.flush();
        }
        return result;
    }
    
    @Override
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(out));
        try {
            write(stream, fileType, output);
        } finally {
            output.close();
        }
        return (int) out.length();
    }

    private <E> E getProperty(AudioFormat format, String key, Class<E> type, E defaultValue) {
        Object value = format.getProperty(key);
        if (value == null)
            return defaultValue;
        if (type.isInstance(value)) {
            @SuppressWarnings("unchecked")
            E result = (E) value;
            return result;
        }
        return defaultValue;
    }

    private static short[] bytesToShorts(byte[] input, int offset, int length) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(input, offset, length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        short[] processedValues = new short[length / 2];
        shortBuffer.get(processedValues);
        return processedValues;
        
//        for (int c = 0; c < processedValues.length; c++) {
//            short a = (short) (((int) input[(c * 2) + offset]) & 0xFF);
//            short b = (short) (((int) input[(c * 2) + 1 + offset]) << 8);
//            processedValues[c] = (short) (a | b);
//        }
//
//        return processedValues;
    }

}
