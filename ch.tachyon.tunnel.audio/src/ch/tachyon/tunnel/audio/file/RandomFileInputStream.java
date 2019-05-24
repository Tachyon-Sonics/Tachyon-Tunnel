package ch.tachyon.tunnel.audio.file;

import java.io.*;
import ch.tachyon.tunnel.audio.utils.*;

/**
 * {@link InputStream} implementation that uses a {@link RandomAccessFile}.
 * This class supports {@link #mark(int)}, and never invalidates a mark, even if you
 * read past the given limit. Hence {@link #reset()} always succeeds.
 * <p>
 * The class optionally support attaching an {@link IProgressMonitor} to monitor the progress
 * of reading the file. Note that the progress is based on the current position in the file.
 * When using {@link #mark(int)} and {@link #reset()} it might go backward.
 */
public class RandomFileInputStream extends InputStream {

    private final RandomAccessFile raFile;
    private final IProgressMonitor progress;
    private long markPos = -1;
    private boolean closed = false;


    public RandomFileInputStream(File file) throws IOException {
        this(file, null);
    }
    
    public RandomFileInputStream(File file, IProgressMonitor progress) throws IOException {
        this.raFile = new RandomAccessFile(file, "r");
        this.progress = progress;
    }

    @Override
    public int read() throws IOException {
        updateProgress();
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
        updateProgress();
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
        updateProgress();
        return raFile.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        updateProgress();
        return raFile.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        if (markPos == -2)
            throw new IOException("mark() failed to get file pointer");
        else if (markPos == -1)
            throw new IOException("No mark()");
        raFile.seek(markPos);
        updateProgress();
    }

    @Override
    public long skip(long n) throws IOException {
        updateProgress();
        return raFile.skipBytes((int) n);
    }
    
    private void updateProgress() throws IOException {
        if (progress == null)
            return;
        double pos = (double) raFile.getFilePointer() / (double) raFile.length();
        progress.setProgress(pos, 1.0);
    }

}
