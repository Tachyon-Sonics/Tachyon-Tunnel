package ch.tachyon.tritonus.test.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class ZeroPaddedFileOutputStream extends FileOutputStream {

    private final long stopAtLength;
    private long position;
    private long length;


    public ZeroPaddedFileOutputStream(File file, long stopAtLength) throws FileNotFoundException {
        super(file);
        this.stopAtLength = stopAtLength;
    }

    @Override
    public void write(int b) throws IOException {
        if (length < stopAtLength)
            super.write(b);
        position++;
        if (position > length)
            length = position;
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (byte e : b)
            write(e);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++)
            write(b[off + i]);
    }


    class FileChannelWrapper extends FileChannel {

        private final FileChannel target;


        public FileChannelWrapper(FileChannel target) {
            this.target = target;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position() throws IOException {
            return position;
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            position = newPosition;
            return target.position(newPosition);
        }

        @Override
        public long size() throws IOException {
            return length;
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void force(boolean metaData) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void implCloseChannel() throws IOException {
            throw new UnsupportedOperationException();
        }

    }


    @Override
    public FileChannel getChannel() {
        FileChannel channel = super.getChannel();
        return new FileChannelWrapper(channel);
    }

    public long getLength() {
        return this.length;
    }

}
