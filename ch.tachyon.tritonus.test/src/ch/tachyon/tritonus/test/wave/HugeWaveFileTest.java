package ch.tachyon.tritonus.test.wave;

import java.io.*;

import javax.sound.sampled.*;

import org.junit.*;
import org.tritonus.share.sampled.*;

public class HugeWaveFileTest {

    @Test
    public void testWriteHugeWaveFiles32bit6Chans192() throws IOException, UnsupportedAudioFileException {
        long frameLength = (1L << 32) + 12345;
        LargeWaveFileTest.doTestWriteLargeAudioFile(frameLength, 6, 192000.0f, 32, Encodings.PCM_FLOAT, AudioFileFormat.Type.WAVE,
                WaveExtraChunkType.DS64);
    }

}
