package ch.tachyon.tunnel.audio.file;

import java.io.*;

import org.tritonus.share.sampled.file.*;

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
public class RandomFileProgressInputStream extends RandomFileInputStream {

    private final IProgressMonitor progress;
    private boolean closed;


    public RandomFileProgressInputStream(File file) throws IOException {
        this(file, null);
    }
    
    public RandomFileProgressInputStream(File file, IProgressMonitor progress) throws IOException {
        super(file);
        this.progress = progress;
    }

    @Override
    public int read() throws IOException {
        updateProgress();
        return super.read();
    }

    @Override
    public void close() throws IOException {
        updateProgress();
        super.close();
        closed = true;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        updateProgress();
        return super.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        updateProgress();
        return super.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        updateProgress();
    }

    @Override
    public long skip(long n) throws IOException {
        updateProgress();
        return super.skip(n);
    }
    
    private void updateProgress() throws IOException {
        if (progress == null || closed)
            return;
        double pos = (double) super.getFilePointer() / (double) super.getFileSize();
        progress.setProgress(pos, 1.0);
    }

}
