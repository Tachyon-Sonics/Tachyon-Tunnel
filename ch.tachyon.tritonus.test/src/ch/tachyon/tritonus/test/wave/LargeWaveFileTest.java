package ch.tachyon.tritonus.test.wave;

import java.io.*;
import java.nio.file.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;

import org.junit.*;
import org.tritonus.share.sampled.*;
import ch.tachyon.tritonus.test.util.ConsoleProgressMonitor;
import ch.tachyon.tritonus.test.util.TestAudioSource;
import ch.tachyon.tritonus.test.util.ZeroPaddedFileOutputStream;
import ch.tachyon.tunnel.audio.file.*;
import ch.tachyon.tunnel.audio.file.AudioFileTypes;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;

public class LargeWaveFileTest {

    @Test
    public void testWriteLargeWaveFiles16bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 33) + 12345;
        // byte size above 32-bit, frame size between 31 and 32 bit (negative when returned as int)
        doTestWriteLargeAudioFile(byteSize / 4, 2, 44100.0f, 16, Encoding.PCM_SIGNED, AudioFileFormat.Type.WAVE,
                WaveExtraChunkType.DS64);
    }

    @Test
    public void testWriteLargeWaveFiles32bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 32) + 12345;
        // byte size above 32-bit, but frame size below 31 bit
        doTestWriteLargeAudioFile(byteSize / 8, 2, 44100.0f, 32, Encodings.PCM_FLOAT, AudioFileFormat.Type.WAVE,
                WaveExtraChunkType.DS64);
    }

    @Test
    public void testWriteVeryLargeWaveFiles16bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 32) + 12345;
        doTestWriteLargeAudioFile(byteSize, 2, 44100.0f, 16, Encoding.PCM_SIGNED, AudioFileFormat.Type.WAVE,
                WaveExtraChunkType.DS64);
    }

    @Test
    public void testWriteVeryLargeWaveFiles32bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 32) + 12345;
        doTestWriteLargeAudioFile(byteSize, 2, 44100.0f, 32, Encodings.PCM_FLOAT, AudioFileFormat.Type.WAVE,
                WaveExtraChunkType.DS64);
    }

    static void doTestWriteLargeAudioFile(long length, int nbChans, float sampleRate, int bits, Encoding encoding,
            AudioFileFormat.Type type, WaveExtraChunkType extraChunk) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".wav");
        long fileLength = -1;
        {
            AudioFormat format = new AudioFormat(encoding, sampleRate, bits, nbChans, bits * nbChans / 8,
                    sampleRate, false);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            System.out.println(fileFormat.toString());
            IProgressMonitor progress = new ConsoleProgressMonitor();
            IMultiChanAudioSource audioSource = new TestAudioSource(length, nbChans, sampleRate, bits, progress);

            ZeroPaddedFileOutputStream output = new ZeroPaddedFileOutputStream(outputFile, 1000);
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, progress);
            fileLength = output.getLength();
        }
        {
            // No need to zero pad here as we will not read past the initial headers
            AudioFileFormat fileFormat = AudioFileTypes.instance().getAudioFileFormat(outputFile, false);
            AudioFormat format = fileFormat.getFormat();
            if (length > 0x7FFFFFFFL || fileFormat instanceof AudioFileFormatEx) {
                AudioFileFormatEx fileFormatEx = (AudioFileFormatEx) fileFormat;
                assert fileFormatEx.getFrameLengthLong() == length;
            }
            assert fileFormat.getFrameLength() == crop(length);
            assert fileFormat.getType() == type;
            assert format.getChannels() == nbChans;
            assert format.getSampleRate() == sampleRate;
            assert format.getFrameRate() == sampleRate;
            assert format.getEncoding().equals(encoding);
            assert format.getSampleSizeInBits() == bits;

            WaveFileUtils.checkContent(outputFile, fileLength, length, extraChunk);
        }
        Files.delete(outputFile.toPath());
    }

    private static int crop(long length) {
        if (length > 0x7FFFFFFFL)
            return -1;
        return (int) length;
    }


}
