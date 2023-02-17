package ch.tachyon.tritonus.test.wave;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;

import org.junit.*;
import org.tritonus.share.sampled.file.*;

import ch.tachyon.tritonus.test.util.TestAudioSource;
import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.common.*;

public class WaveFileTest {

    private static final int AUDIO_FRAME_LENGTH = 10000;
	private final List<AudioFormat> formats;

    public WaveFileTest() {
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
    public void testWriteWaveFiles() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFile(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.WAVE);
        }
    }

    @Test
    public void testWriteWaveFileOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.FileOutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.WAVE);
        }
    }

    @Test
    public void testWriteWaveOtherOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using other OutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioOutputStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.WAVE);
        }
    }

    /**
     * Test patching the header with a {@link File}
     */
    @Test
    public void testWriteWaveFilesUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.WAVE);
        }
    }

    /**
     * Test patching the header with a {@link FileOutputStream}
     */
    @Test
    public void testWriteWaveFileStreamsUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using FileOutputStream, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStreamUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.WAVE);
        }
    }

    private void doTestWriteAudioFile(long length, AudioFormat format,
            AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, WaveExtraChunkType.NONE, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, WaveExtraChunkType.NONE, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioOutputStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, null);
        }

        checkResult(length, format, type, WaveExtraChunkType.NONE, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, WaveExtraChunkType.JUNK, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, WaveExtraChunkType.JUNK, outputFile);
        Files.delete(outputFile.toPath());
    }

    private void checkResult(long length, AudioFormat expectedFormat, AudioFileFormat.Type type,
            WaveExtraChunkType extraChunk, File outputFile)
            throws IOException, UnsupportedAudioFileException, FileNotFoundException {
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

        WaveFileUtils.checkContent(outputFile, outputFile.length(), length, extraChunk);
    }

}
