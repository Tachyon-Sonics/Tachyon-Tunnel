package org.tritonus.share.sampled.file;

import java.io.*;

/**
 * {@link InputStream} implementation that uses a {@link RandomAccessFile}.
 * This class supports {@link #mark(int)}, and never invalidates a mark, even if you
 * read past the given limit. Hence {@link #reset()} always succeeds.
 * <p>
 * The JavaSound system is based on {@link #mark(int)} and {@link #reset()} to probe
 * each possible audio formats. However, for some files, the 'readLimit' passed to
 * {@link #mark(int)} is not sufficient (for example some .mp3 file with a huge PDF leaflet
 * in the meta-data). Such files fail to load with a {@link FileInputStream}, but work
 * properly with a {@link RandomFileInputStream}.
 */
public class RandomFileInputStream extends InputStream {

    private final RandomAccessFile raFile;
    private long markPos = -1;
    private boolean closed = false;


    public RandomFileInputStream(File file) throws IOException {
        this.raFile = new RandomAccessFile(file, "r");
    }

    @Override
    public int read() throws IOException {
        return raFile.read();
    }

    @Override
    public int available() throws IOException {
        return (int) (raFile.length() - raFile.getFilePointer());
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        raFile.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        try {
            this.markPos = raFile.getFilePointer();
        } catch (IOException ex) {
            this.markPos = -2;
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raFile.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return raFile.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        if (markPos == -2)
            throw new IOException("mark() failed to get file pointer");
        else if (markPos == -1)
            throw new IOException("No mark()");
        raFile.seek(markPos);
    }

    @Override
    public long skip(long n) throws IOException {
        return raFile.skipBytes((int) n);
    }
    
    public long getFilePointer() throws IOException {
        return raFile.getFilePointer();
    }
    
    public long getFileSize() throws IOException {
        return raFile.length();
    }
    
    protected RandomAccessFile getFile() {
        return raFile;
    }
    
}
