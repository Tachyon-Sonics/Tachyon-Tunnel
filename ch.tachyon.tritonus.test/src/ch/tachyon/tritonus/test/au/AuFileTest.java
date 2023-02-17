package ch.tachyon.tritonus.test.au;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        formats.add(new AudioFormat(encoding, sampleRate, nbBits, nbChans, nbBits * nbChans / 8, sampleRate, false));
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

        checkResult(length, format, type, outputFile, false);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStream(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false);
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

        checkResult(length, format, type, outputFile, false);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, outputFile, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioFileStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            AudioFileIO.saveAudioFile(audioSource, new FileOutputStream(outputFile), fileFormat, null);
        }

        checkResult(length, format, type, outputFile, false);
        Files.delete(outputFile.toPath());
    }

    private void doTestWriteAudioOtherStreamUnknownLength(long length, AudioFormat format, AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        {
            IMultiChanAudioSource audioSource = new TestAudioSource(length, format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits(), null);
            AudioFileFormat fileFormat = new AudioFileFormat(type, format, AudioSystem.NOT_SPECIFIED);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, null);
        }

        checkResult(length, format, type, outputFile, true);
        Files.delete(outputFile.toPath());
    }

    private void checkResult(long length, AudioFormat expectedFormat, AudioFileFormat.Type type, File outputFile,
    		boolean unknownLength)
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
        
        // Check content
        FileInputStream fInput = new FileInputStream(outputFile);
        DataInputStream input = new DataInputStream(fInput);
        
        String fileId = readChunkId(input);
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
        for (int i = 0; i < toSkip; i++) {
        	int value = input.read();
        	System.out.print((char) value);
        }
        System.out.println();
        assert fInput.getChannel().position() + dataLength == fileLength;
        input.close();
        
        // TODO [qt] AU test large and huge files
    }

    private static String readChunkId(InputStream input) throws IOException {
        long b0 = input.read();
        long b1 = input.read();
        long b2 = input.read();
        long b3 = input.read();
        if ((b0 | b1 | b2 | b3) < 0)
            throw new EOFException();
        return "" + (char) b0 + (char) b1 + (char) b2 + (char) b3;
    }


}
