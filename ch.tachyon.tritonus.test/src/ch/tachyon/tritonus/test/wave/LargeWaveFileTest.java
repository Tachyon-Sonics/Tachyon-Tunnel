package ch.tachyon.tritonus.test.wave;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;

import org.junit.*;
import org.tritonus.sampled.file.*;
import org.tritonus.share.sampled.*;
import org.tritonus.share.sampled.file.*;

import ch.tachyon.tritonus.test.util.ConsoleProgressMonitor;
import ch.tachyon.tritonus.test.util.TestAudioSource;
import ch.tachyon.tritonus.test.util.ZeroPaddedRandomAccessFile;
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
        AtomicLong fileLength = new AtomicLong();
        {
            AudioFormat format = new AudioFormat(encoding, sampleRate, bits, nbChans, bits * nbChans / 8,
                    sampleRate, false);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            System.out.println(fileFormat.toString());
            IProgressMonitor progress = new ConsoleProgressMonitor();
            IMultiChanAudioSource audioSource = new TestAudioSource(length, nbChans, sampleRate, bits, progress);

            AudioInputStream input = AudioSourceInputStream.createAudioInputStream(audioSource, fileFormat.getFormat(),
                    null, length);
            WaveAudioFileWriter writer = new WaveAudioFileWriter() {

                /**
                 * Overriden so we can replace the output stream using a version based on {@link ZeroPaddedRandomAccessFile}
                 * to prevent actually writing a huge file.
                 */
                @Override
                protected int writeImpl(AudioInputStream audioInputStream, AudioOutputStream audioOutputStream, boolean bNeedsConversion) throws IOException {
                    AudioFormat format = audioOutputStream.getFormat();
                    long length = audioOutputStream.getLength();
                    audioOutputStream.close(); // Close previous file
                    // Reopen as a zero padded file
                    ZeroPaddedRandomAccessFile zpraf = new ZeroPaddedRandomAccessFile(outputFile, 1000, 0);
                    // Create patched output stream
                    WaveAudioOutputStream mockStream = new WaveAudioOutputStream(format, length, zpraf);
                    int result = super.writeImpl(audioInputStream, mockStream, bNeedsConversion);
                    fileLength.set(zpraf.length());
                    return result;
                }

            };
            writer.write(input, fileFormat.getType(), outputFile);
        }
        {
            // No need to patch here as we will not read past the initial headers
            AudioFileFormat fileFormat = AudioFileTypes.instance().getAudioFileFormat(outputFile, false);
            AudioFormat format = fileFormat.getFormat();
            if (length > 0x7FFFFFFFL) {
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

            WaveFileUtils.checkContent(outputFile, fileLength.get(), length, extraChunk);
        }
        Files.delete(outputFile.toPath());
    }

    private static int crop(long length) {
        if (length > 0x7FFFFFFFL)
            return -1;
        return (int) length;
    }


}
