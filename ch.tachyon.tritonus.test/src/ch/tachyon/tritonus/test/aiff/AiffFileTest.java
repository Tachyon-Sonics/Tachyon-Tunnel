package ch.tachyon.tritonus.test.aiff;

import java.io.BufferedOutputStream;
import java.io.File;
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
import org.tritonus.share.sampled.file.TAudioFileFormat;

import ch.tachyon.tritonus.test.util.FfmpegAudioFileChecker;
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
        formats.add(new AudioFormat(encoding, sampleRate, nbBits, nbChans, nbBits * nbChans / 8, sampleRate, false));
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
        TAudioFileFormat fileFormat = (TAudioFileFormat) AudioFileTypes.instance().getAudioFileFormat(outputFile, false);
        AudioFormat format = fileFormat.getFormat();
        assert fileFormat.getFrameLengthEx() == length;
        assert fileFormat.getFrameLength() == (int) length;
        assert fileFormat.getType() == type;
        assert format.getChannels() == expectedFormat.getChannels();
        assert format.getSampleRate() == expectedFormat.getSampleRate();
        assert format.getFrameRate() == expectedFormat.getFrameRate();
        assert format.getEncoding().equals(expectedFormat.getEncoding());
        assert format.getSampleSizeInBits() == expectedFormat.getSampleSizeInBits();

        // TODO [qt] AIFF check content?
        
        // Check with ffmpeg
        FfmpegAudioFileChecker.assertAudioFileSane(outputFile);
    }


}
