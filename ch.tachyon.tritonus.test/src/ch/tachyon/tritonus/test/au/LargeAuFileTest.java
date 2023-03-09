package ch.tachyon.tritonus.test.au;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.Test;
import org.tritonus.share.sampled.Encodings;
import ch.tachyon.tritonus.test.util.ConsoleProgressMonitor;
import ch.tachyon.tritonus.test.util.TestAudioSource;
import ch.tachyon.tritonus.test.util.ZeroPaddedFileOutputStream;
import ch.tachyon.tritonus.test.util.ZeroPaddedRandomInputStream;
import ch.tachyon.tunnel.audio.file.AudioFileFormatEx;
import ch.tachyon.tunnel.audio.file.AudioFileIO;
import ch.tachyon.tunnel.audio.file.AudioFileTypes;
import ch.tachyon.tunnel.audio.utils.IProgressMonitor;
import ch.tachyon.tunnel.common.IMultiChanAudioSource;

public class LargeAuFileTest {

    @Test
    public void testWriteLargeAuFile16bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 31) + 12345;
        // byte size between 2^31 and 2^32, should result in unknown size as size is signed
        doTestWriteLargeAudioFile(byteSize / 4, 2, 44100.0f, 16, Encoding.PCM_SIGNED, AudioFileFormat.Type.AU);
    }

    @Test
    public void testWriteVeryLargeAuFile32bit() throws IOException, UnsupportedAudioFileException {
        long byteSize = (1L << 32) + 12345;
        // byte size above 2^32
        doTestWriteLargeAudioFile(byteSize / (4 * 6), 6, 192000.0f, 32, Encodings.PCM_IEEE_FLOAT, AudioFileFormat.Type.AU);
    }

    @Test
    public void testWriteHugeAuFile16bit() throws IOException, UnsupportedAudioFileException {
        long frameSize = (1L << 32) + 12345;
        // frame size above 2^32
        doTestWriteLargeAudioFile(frameSize, 2, 44100.0f, 8, Encodings.PCM_SIGNED, AudioFileFormat.Type.AU);
    }

    /**
     * @param length in audio sample frames
     */
    static void doTestWriteLargeAudioFile(long length, int nbChans, float sampleRate, int bits, Encoding encoding,
            AudioFileFormat.Type type) throws IOException, UnsupportedAudioFileException {
        File outputFile = File.createTempFile("Output", ".au");
        long fileLength = -1;
        {
            AudioFormat format = new AudioFormat(encoding, sampleRate, bits, nbChans, bits * nbChans / 8,
                    sampleRate, true);
            AudioFileFormat fileFormat = new AudioFileFormatEx(type, format, length);
            System.out.println(fileFormat.toString());
            IProgressMonitor progress = new ConsoleProgressMonitor();
            IMultiChanAudioSource audioSource = new TestAudioSource(length, nbChans, sampleRate, bits, progress);

            ZeroPaddedFileOutputStream output = new ZeroPaddedFileOutputStream(outputFile, 1000);
            AudioFileIO.saveAudioFile(audioSource, output, fileFormat, progress);
            fileLength = output.getLength();
        }
        {
            // Create a stream that properly fakes the file length, as it is used to get the frame length
            InputStream input = new ZeroPaddedRandomInputStream(outputFile, 1000, fileLength);

            // No need to patch here as we will not read past the initial headers
            AudioFileFormat fileFormat = AudioFileTypes.instance().getAudioFileFormat("output.au", input);
            AudioFormat format = fileFormat.getFormat();
            if (fileFormat instanceof AudioFileFormatEx || length > 0x7FFFFFFFL || fileLength > 0x7FFFFFFFL) {
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

            input.close();
        }
        Files.delete(outputFile.toPath());
    }

    private static int crop(long length) {
        if (length > 0x7FFFFFFFL)
            return -1;
        return (int) length;
    }

}
