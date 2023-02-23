package ch.tachyon.tritonus.test.aiff;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.Test;
import ch.tachyon.tritonus.test.util.FfmpegAudioFileChecker;
import ch.tachyon.tritonus.test.util.IffUtils;
import ch.tachyon.tritonus.test.util.TestAudioSource;
import ch.tachyon.tunnel.audio.file.AudioFileFormatEx;
import ch.tachyon.tunnel.audio.file.AudioFileIO;
import ch.tachyon.tunnel.audio.file.AudioFileTypes;
import ch.tachyon.tunnel.common.IMultiChanAudioSource;

public class AiffFileTest {

    private static final int AUDIO_FRAME_LENGTH = 10000;
	private final List<AudioFormat> formats;

	
    public AiffFileTest() {
        formats = new ArrayList<>();
        addFormat(2, 44100.0f, 16, Encoding.PCM_SIGNED);
        addFormat(2, 44100.0f, 32, Encoding.PCM_FLOAT);
        addFormat(1, 44100.0f, 16, Encoding.PCM_SIGNED);
        addFormat(1, 44100.0f, 32, Encoding.PCM_FLOAT);
        addFormat(2, 192000.0f, 16, Encoding.PCM_SIGNED);
        addFormat(2, 192000.0f, 32, Encoding.PCM_FLOAT);
        addFormat(2, 192000.0f, 24, Encoding.PCM_SIGNED);

        addFormat(6, 44100.0f, 16, Encoding.PCM_SIGNED);
        addFormat(6, 44100.0f, 32, Encoding.PCM_FLOAT);
        addFormat(6, 192000.0f, 16, Encoding.PCM_SIGNED);
        addFormat(6, 192000.0f, 24, Encoding.PCM_SIGNED);
        addFormat(6, 192000.0f, 32, Encoding.PCM_FLOAT);
    }

    private void addFormat(int nbChans, float sampleRate, int nbBits, Encoding encoding) {
        formats.add(new AudioFormat(encoding, sampleRate, nbBits, nbChans, nbBits * nbChans / 8, sampleRate, true));
    }

    @Test
    public void testWriteAiffFiles() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            if (format.getEncoding().equals(Encoding.PCM_SIGNED))
            	doTestWriteAudioFile(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFF);
            doTestWriteAudioFile(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFC);
        }
    }

    @Test
    public void testWriteAiffFileOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.FileOutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFC);
        }
    }

    @Test
    public void testWriteAiffOtherOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using other OutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioOutputStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFC);
        }
    }

    /**
     * Test patching the header with a {@link File}
     */
    @Test
    public void testWriteAiffFilesUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFC);
        }
    }

    /**
     * Test patching the header with a {@link FileOutputStream}
     */
    @Test
    public void testWriteAiffFileStreamsUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using FileOutputStream, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStreamUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AIFC);
        }
    }

    private void doTestWriteAudioFile(long length, AudioFormat format,
            AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".aifc");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".aifc");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioOutputStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".aifc");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, null);
        }

        checkResult(length, format, type, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".aifc");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".aifc");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void checkResult(long length, AudioFormat expectedFormat, AudioFileFormat.Type type, File outputFile)
            throws IOException, UnsupportedAudioFileException, FileNotFoundException {
    	// Check format
        AudioFileFormatEx fileFormat = (AudioFileFormatEx) AudioFileTypes.instance().getAudioFileFormat(outputFile, false);
        AudioFormat format = fileFormat.getFormat();
        assert fileFormat.getFrameLengthLong() == length;
        assert fileFormat.getFrameLength() == (int) length;
        assert fileFormat.getType() == type;
        assert format.getChannels() == expectedFormat.getChannels();
        assert format.getSampleRate() == expectedFormat.getSampleRate();
        assert format.getFrameRate() == expectedFormat.getFrameRate();
        assert format.getEncoding().equals(expectedFormat.getEncoding());
        assert format.getSampleSizeInBits() == expectedFormat.getSampleSizeInBits();

        // Check content
        FileInputStream fInput = new FileInputStream(outputFile);
        DataInputStream input = new DataInputStream(fInput);
        
        String formId = IffUtils.readChunkId(input);
        assert formId.equals("FORM");
        int fileLength = input.readInt();
        assert fInput.getChannel().size() == fileLength + 8;
        String aiffId = IffUtils.readChunkId(input);
        assert aiffId.equals("AIFF") || aiffId.equals("AIFC");
        if (aiffId.equals("AIFC")) {
        	String fverId = IffUtils.readChunkId(input);
        	assert fverId.equals("FVER");
        	int fverLength = input.readInt();
        	assert fverLength == 4;
        	assert input.readInt() == 0xA2805140;
        }
        
        String commId = IffUtils.readChunkId(input);
        assert commId.equals("COMM");
        int commSize = input.readInt();
        if (aiffId.equals("AIFC"))
        	assert commSize == 24;
        else
        	assert commSize == 18;
        int nbChans = input.readShort();
        assert nbChans == expectedFormat.getChannels();
        int frameLength = input.readInt();
        assert frameLength == length;
        int sizeInBits = input.readShort();
        assert sizeInBits == expectedFormat.getSampleSizeInBits();
        input.skipBytes(10); // Skip sample rate
        if (aiffId.equals("AIFC")) {
        	input.skipBytes(6); // Skip format code & name
        }
        String ssndId = IffUtils.readChunkId(input);
        assert ssndId.equals("SSND");
        int ssndSize = input.readInt();
        assert ssndSize == fInput.getChannel().size() - fInput.getChannel().position();
        int ssndOffset = input.readInt();
        assert ssndOffset == 0;
        int ssndBlockSize = input.readInt();
        assert ssndBlockSize == 0;
        input.close();
        
        // Check with ffmpeg
        FfmpegAudioFileChecker.assertAudioFileSane(outputFile);
    }

}
