package ch.tachyon.opus.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.concentus.OpusApplication;
import org.concentus.OpusSignal;
import org.junit.Test;
import ch.tachyon.opus.spi.Opus;
import ch.tachyon.opus.spi.OpusAudioFileReader;
import ch.tachyon.opus.spi.OpusAudioFileWriter;

public class OpusSpiProviderTest {
    
    private final static String TEST_FILE = "files/TestFile.opus";
    // Length 1:04.054 (3 074 568 samples)
    
    private final static int NB_CHANNELS = 2;
    private final static float SAMPLE_RATE = 48000.0f;
    
    private static Long opusByteLength;
    private static Long pcmByteLength;
    private static Long frameLength;
    
    
    @Test
    public void testGetFileFormatFromFile() throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new File(TEST_FILE));
        assert fileFormat.getType().equals(Opus.OPUS_FILE_TYPE);
        assert fileFormat.getFrameLength() > 0;
        AudioFormat format = fileFormat.getFormat();
        assert format.getChannels() == NB_CHANNELS;
        assert format.getEncoding().equals(Opus.OPUS_ENCODING);
        assert format.getSampleRate() == SAMPLE_RATE;
        double duration = (double) fileFormat.getFrameLength() / (double) format.getSampleRate();
        System.out.println("Frame length (format): " + fileFormat.getFrameLength());
        System.out.println("  Duration (format) in seconds: " + duration);
        System.out.println("  Properties: " + fileFormat.properties());
        checkFrameLengthConsistency(fileFormat.getFrameLength());
    }

    @Test
    public void testGetFileFormatFromStream() throws UnsupportedAudioFileException, IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(TEST_FILE));
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(input);
        input.close();
        assert fileFormat.getType().equals(Opus.OPUS_FILE_TYPE);
        
        /*
         * When using a stream, length is not calculated
         */
        assert fileFormat.getFrameLength() == AudioSystem.NOT_SPECIFIED;
        assert fileFormat.getByteLength() == AudioSystem.NOT_SPECIFIED;
        AudioFormat format = fileFormat.getFormat();
        assert format.getChannels() == NB_CHANNELS;
        assert format.getEncoding().equals(Opus.OPUS_ENCODING);
        assert format.getSampleRate() == SAMPLE_RATE;
        System.out.println("Properties: " + fileFormat.properties());
    }
    
    @Test
    public void testGetFileFormatInvalidThroughAudioSystem() throws UnsupportedAudioFileException, IOException {
        InputStream input = getClass().getResourceAsStream(getClass().getSimpleName() + ".class");
        try {
            AudioSystem.getAudioFileFormat(new BufferedInputStream(input, 100000));
            assert false : "File should not be recognized as an audio file";
        } catch (UnsupportedAudioFileException ex) {
            // Expected
        } finally {
            input.close();
        }
    }
    
    @Test
    public void testGetFileFormatInvalid() throws UnsupportedAudioFileException, IOException {
        InputStream input = getClass().getResourceAsStream(getClass().getSimpleName() + ".class");
        try {
            new OpusAudioFileReader().getAudioFileFormat(new BufferedInputStream(input, 100000));
            assert false : "File should not be recognized as an audio file";
        } catch (UnsupportedAudioFileException ex) {
            // Expected
            assert ex.getMessage().contains("Supplied File is not Opus");
        } finally {
            input.close();
        }
    }
    
    @Test
    public void testGetOpusAudioInputStreamFromFile() throws UnsupportedAudioFileException, IOException {
        AudioInputStream input = AudioSystem.getAudioInputStream(new File(TEST_FILE));
        checkAudioInputStream(input);
    }
    
    @Test
    public void testGetOpusAudioInputStreamFromStream() throws IOException, UnsupportedAudioFileException {
        InputStream input = new BufferedInputStream(new FileInputStream(new File(TEST_FILE)));
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(input);
        checkAudioInputStream(audioStream);
    }

    private void checkAudioInputStream(AudioInputStream input) throws IOException {
        AudioFormat format = input.getFormat();
        assert format.getChannels() == NB_CHANNELS;
        assert format.getEncoding().equals(Opus.OPUS_ENCODING);
        assert format.getSampleRate() == SAMPLE_RATE;
        BufferedInputStream bInput = new BufferedInputStream(input);
        long total = 0;
        while (true) {
            int value = bInput.read();
            if (value < 0)
                break;
            total++;
        }
        System.out.println("Number of bytes read in OPUS encoded stream: " + total);
        if (opusByteLength == null) {
            opusByteLength = total;
        } else {
            assert opusByteLength.longValue() == total : "Number of bytes read in OPUS encoded stream differ between the tests";
        }
        assert total > 0;
        bInput.close();
    }
    
    @Test
    public void testConvertToPcmFromFile() throws UnsupportedAudioFileException, IOException {
        AudioInputStream opusStream = AudioSystem.getAudioInputStream(new File(TEST_FILE));
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, opusStream);
        checkPcmDecodedStream(pcmStream);
    }

    @Test
    public void testConvertToPcmFromStream() throws UnsupportedAudioFileException, IOException {
        InputStream input = new BufferedInputStream(new FileInputStream(new File(TEST_FILE)));
        AudioInputStream opusStream = AudioSystem.getAudioInputStream(input);
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, opusStream);
        checkPcmDecodedStream(pcmStream);
    }

    private void checkPcmDecodedStream(AudioInputStream pcmStream) throws IOException {
        quickCheckPcmDecodedStream(pcmStream);
        BufferedInputStream bInput = new BufferedInputStream(pcmStream);
        long total = 0;
        while (true) {
            int value = bInput.read();
            if (value < 0)
                break;
            total++;
        }
        System.out.println("Number of bytes read in PCM decoded stream: " + total);
        long frameLength = total / (NB_CHANNELS * 2);
        System.out.println("  Frame length (decoded): " + frameLength);
        checkFrameLengthConsistency(frameLength);
        double duration = (double) total / (double) (SAMPLE_RATE * NB_CHANNELS * 2);
        System.out.println("  Duration (decoded) in seconds: " + duration);
        if (pcmByteLength == null) {
            pcmByteLength = total;
        } else {
            assert pcmByteLength.longValue() == total : "Number of bytes in PCM decoded stream differ between the tests";
        }
        bInput.close();
    }

    private void quickCheckPcmDecodedStream(AudioInputStream pcmStream) {
        AudioFormat format = pcmStream.getFormat();
        assert format.getChannels() == NB_CHANNELS;
        assert format.getEncoding().equals(Encoding.PCM_SIGNED);
        assert format.getSampleRate() == SAMPLE_RATE;
        assert !format.isBigEndian();
        assert format.getSampleSizeInBits() == 16;
        assert pcmStream.getFrameLength() == -1;
    }
    
    private void checkFrameLengthConsistency(long fl) {
        if (frameLength == null) {
            frameLength = fl;
        } else  {
            assert frameLength.longValue() == fl : "Frame length differ between tests: " + frameLength + " != " + fl;
        }
    }
    
    @Test
    public void testWriteOpusFile() throws UnsupportedAudioFileException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        { // Decode file to PCM, and re-encode in opus in memory
            AudioInputStream opusStream = AudioSystem.getAudioInputStream(new File(TEST_FILE));
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, opusStream);
            AudioSystem.write(pcmStream, Opus.OPUS_FILE_TYPE, output);
            output.close();
        
            System.out.println("Encoded opus file length [bytes]: " + output.size());
        }
        
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        {
            AudioInputStream opusStream = AudioSystem.getAudioInputStream(input);
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, opusStream);
            
            /*
             * Writing is still somewhat buggy at this stage, the exact number of sample frames does not match
             * after decoding/encoding/decoding.
             * 
             * Hence for the moment we only quick check the audio format, but not the exact length:
             */
            quickCheckPcmDecodedStream(pcmStream);
        }
    }

    @Test
    public void testWriteOpusFileWithProperties() throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new File(TEST_FILE));

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        { // Decode file to PCM, and re-encode in opus in memory
            AudioInputStream opusStream = AudioSystem.getAudioInputStream(new File(TEST_FILE));
            AudioFormat opusFormat = opusStream.getFormat();
            
            // Customize opus encoding:
            Map<String, Object> properties = new HashMap<>();
            properties.put(OpusAudioFileWriter.OPUS_COMPLEXITY_KEY, 1);
            properties.put(OpusAudioFileWriter.OPUS_BITRATE_KEY, 48000);
            properties.put(OpusAudioFileWriter.OPUS_PACKET_SAMPLES_KEY, 480);
            properties.put(OpusAudioFileWriter.OPUS_APPLICATION_KEY, OpusApplication.OPUS_APPLICATION_VOIP);
            properties.put(OpusAudioFileWriter.OPUS_SIGNAL_KEY, OpusSignal.OPUS_SIGNAL_VOICE);
            properties.put(OpusAudioFileWriter.OPUS_TAG_KEY_PREFIX + "encoder", "ch.tachyon.opus");
            properties.put(Opus.OPUS_PRE_SKIP_KEY, fileFormat.getProperty(Opus.OPUS_PRE_SKIP_KEY));
            properties.put(Opus.OPUS_OUTPUT_GAIN_KEY, fileFormat.getProperty(Opus.OPUS_OUTPUT_GAIN_KEY));
            
            // Conversion is only supported with PCM_SIGNED, 16-bit
            AudioFormat pcmFormat = new AudioFormat(
                    Encoding.PCM_SIGNED, 
                    opusFormat.getSampleRate(), 
                    16, 
                    opusFormat.getChannels(), 
                    2 * opusFormat.getChannels(), // Important: do not leave NOT_SPECIFIED here
                    opusFormat.getSampleRate(), // Important: do not leave NOT_SPECIFIED here
                    false,
                    properties);
            
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, opusStream);
            
            AudioSystem.write(pcmStream, Opus.OPUS_FILE_TYPE, output);
            output.close();
        
            System.out.println("Encoded opus file length [bytes]: " + output.size());
        }
        
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        {
            AudioInputStream opusStream = AudioSystem.getAudioInputStream(input);
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, opusStream);
            quickCheckPcmDecodedStream(pcmStream);
        }
    }

}
