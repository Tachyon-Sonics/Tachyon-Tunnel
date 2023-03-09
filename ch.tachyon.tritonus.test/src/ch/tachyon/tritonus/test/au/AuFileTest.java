package ch.tachyon.tritonus.test.au;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class AuFileTest {


    private static final int AUDIO_FRAME_LENGTH = 10000;
    private final List<AudioFormat> formats;


    public AuFileTest() {
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
    public void testWriteAudioFiles() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFile(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    @Test
    public void testWriteAudioFileOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.FileOutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    @Test
    public void testWriteAudioOtherOutputStreams() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using other OutputStream, known length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioOutputStream(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    /**
     * Test patching the header with a {@link File}
     */
    @Test
    public void testWriteAudioFilesUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using java.io.File, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    /**
     * Test patching the header with a {@link FileOutputStream}
     */
    @Test
    public void testWriteAudioFileStreamsUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using FileOutputStream, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioFileStreamUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    /**
     * Test unknown length. Header will NOT be patched
     */
    @Test
    public void testWriteOtherFileStreamsUnknownLength() throws IOException, UnsupportedAudioFileException {
        System.out.println("Using other OutputStream, unknown length");
        for (AudioFormat format : formats) {
            System.out.println("  " + format.toString());
            doTestWriteAudioOtherStreamUnknownLength(AUDIO_FRAME_LENGTH, format, AudioFileFormat.Type.AU);
        }
    }

    private void doTestWriteAudioFile(long length, AudioFormat format,
            AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false, "Created by Tritonus");
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            Map<String, Object> properties = new HashMap<>();
            properties.put("title", "Test Song Title");
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length, properties);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false, "Test Song Title");
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioOutputStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false, "Created by Tritonus");
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type)
            throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false, "Created by Tritonus");
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type)
            throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false, "Created by Tritonus");
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioOtherStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type)
            throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, true, "Created by Tritonus");
        Files.delete(outputFile.toPath());
    }

    private void checkResult(long length, AudioFormat expectedFormat, AudioFileFormat.Type type, File outputFile,
            boolean unknownLength, String songTitle)
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

        String fileId = IffUtils.readChunkId(input);
        assert fileId.equals(".snd");
        int dataOffset = input.readInt();
        int fLength = input.readInt();
        long fileLength = outputFile.length();
        long dataLength = fileLength - dataOffset;
        if (dataLength <= 0x7FFFFFFFl && !unknownLength)
            assert fLength == dataLength;
        else
            assert fLength == AudioSystem.NOT_SPECIFIED;
        input.readInt(); // Format code
        int sampleRate = input.readInt();
        assert sampleRate == (int) format.getSampleRate();
        int channels = input.readInt();
        assert channels == format.getChannels();
        int toSkip = dataOffset - (int) fInput.getChannel().position();
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 0; i < toSkip - 1; i++) {
            int value = input.read();
            titleBuilder.append((char) value);
        }
        assert input.read() == 0; // null-terminated string
        assert titleBuilder.toString().equals(songTitle);
        assert fInput.getChannel().position() + dataLength == fileLength;
        input.close();

        // Check with ffmpeg
        FfmpegAudioFileChecker.assertAudioFileSane(outputFile);
    }

}
