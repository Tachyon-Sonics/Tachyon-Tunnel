package ch.tachyon.tritonus.test.wave;

import java.io.*;

import javax.sound.sampled.*;

import org.tritonus.sampled.file.*;

class WaveFileUtils {

    static String readChunkId(InputStream input) throws IOException {
        long b0 = input.read();
        long b1 = input.read();
        long b2 = input.read();
        long b3 = input.read();
        if ((b0 | b1 | b2 | b3) < 0)
            throw new EOFException();
        return "" + (char) b0 + (char) b1 + (char) b2 + (char) b3;
    }

    static int readLittleEndianInt(InputStream input) throws IOException {
        int b0 = input.read();
        int b1 = input.read();
        int b2 = input.read();
        int b3 = input.read();
        if ((b0 | b1 | b2 | b3) < 0)
            throw new EOFException();
        return (b3 << 24) + (b2 << 16) + (b1 << 8) + (b0 << 0);
    }

    static long readLittleEndianLong(InputStream is) throws IOException {
        long b0 = is.read();
        long b1 = is.read();
        long b2 = is.read();
        long b3 = is.read();
        long b4 = is.read();
        long b5 = is.read();
        long b6 = is.read();
        long b7 = is.read();
        if ((b0 | b1 | b2 | b3 | b4 | b5 | b6 | b7) < 0)
            throw new EOFException();
        return (b7 << 56) + (b6 << 48) + (b5 << 40) + (b4 << 32) + (b3 << 24) + (b2 << 16) + (b1 << 8) + (b0 << 0);
    }

    static void skip(InputStream input, long amount) throws IOException {
        while (amount > 0) {
            long skipped = input.skip(amount);
            if (skipped <= 0)
                throw new EOFException();
            amount -= skipped;
        }
    }

    static void checkContent(File outputFile, long fileLength, long frameLength,
            WaveExtraChunkType extraChunk) throws FileNotFoundException, IOException {
        // Check RIFF size value
        FileInputStream input = new FileInputStream(outputFile);
        String riffId = WaveFileUtils.readChunkId(input);
        if (extraChunk == WaveExtraChunkType.DS64)
            assert riffId.equals("RF64");
        else
            assert riffId.equals("RIFF");
        int riffLength = readLittleEndianInt(input);
        if (fileLength - 8 <= Integer.MAX_VALUE)
            assert ((long) riffLength & 0xFFFFFFFFL) == fileLength - 8;
        else
            assert riffLength == AudioSystem.NOT_SPECIFIED;

        String waveId = readChunkId(input);
        assert waveId.equals("WAVE");

        Ds64Chunk ds64Chunk = null;
        if (extraChunk == WaveExtraChunkType.JUNK) {
            String junkId = readChunkId(input);
            assert junkId.equals("JUNK");
            int ckLength = readLittleEndianInt(input);
            skip(input, ckLength);
        } else if (extraChunk == WaveExtraChunkType.DS64) {
            String ds64Id = readChunkId(input);
            assert ds64Id.equals("ds64");
            int ckLength = readLittleEndianInt(input);
            assert ckLength == 28;
            long riffLengthL = readLittleEndianLong(input);
            assert riffLengthL == fileLength - 8;
            long dataLengthL = readLittleEndianLong(input);
            long frameLengthL = readLittleEndianLong(input);
            assert frameLengthL == frameLength;
            int tableLength = readLittleEndianInt(input);
            assert tableLength == 0;
            ds64Chunk = new Ds64Chunk(riffLengthL, dataLengthL, frameLengthL);
        }

        String fmtId = readChunkId(input);
        assert fmtId.equals("fmt ");
        int ckLength = readLittleEndianInt(input);
        skip(input, ckLength);

        String dataId = readChunkId(input);
        assert dataId.equals("data");
        ckLength = readLittleEndianInt(input);

        if (extraChunk == WaveExtraChunkType.DS64) {
            /*
             * This file was created using a ZeroPaddedRandomAccessFile. We cannot read it entirely.
             * Just check that the end of the data chunk matches the fictive length.
             */
            assert ckLength == AudioSystem.NOT_SPECIFIED;
            assert input.getChannel().position() + ds64Chunk.getDataSize() == fileLength;
        } else {
            assert ckLength > 0;
            skip(input, ckLength);
            assert input.read() < 0 : "Should have reached eof";
        }

        input.close();
    }

}
