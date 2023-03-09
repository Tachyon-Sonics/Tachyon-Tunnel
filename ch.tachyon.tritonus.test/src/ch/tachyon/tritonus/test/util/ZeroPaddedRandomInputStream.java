package ch.tachyon.tritonus.test.util;

import java.io.File;
import java.io.IOException;

import org.tritonus.share.sampled.file.RandomFileInputStream;

public class ZeroPaddedRandomInputStream extends RandomFileInputStream {

    private final long stopAtLength;
    private final long length;
    private long position;
    private long markPosition;


    public ZeroPaddedRandomInputStream(File file, long stopAtLength, long length) throws IOException {
        super(file);
        this.stopAtLength = stopAtLength;
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        if (position < stopAtLength) {
            int result = super.read();
            if (result >= 0)
                position++;
            return result;
        } else if (position < length) {
            position++;
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = 0;
        for (int i = 0; i < len; i++) {
            int value = read();
            if (value < 0)
                return (result == 0 ? -1 : result);
            b[off + i] = (byte) value;
            position++;
        }
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized void mark(int readlimit) {
        markPosition = position;
        if (position < stopAtLength)
            super.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        position = markPosition;
        if (markPosition < stopAtLength)
            super.reset();
        markPosition = 0;
    }

    @Override
    public long getFilePointer() throws IOException {
        return position;
    }

    @Override
    public long getFileSize() throws IOException {
        return length;
    }

}
