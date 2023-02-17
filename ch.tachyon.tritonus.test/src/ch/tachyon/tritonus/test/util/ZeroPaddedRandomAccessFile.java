package ch.tachyon.tritonus.test.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.tritonus.share.sampled.file.TSeekableDataOutputStream;

/**
 * A warpper around {@link TSeekableDataOutputStream} to test huge files without actually creating them
 * fully on the disk.
 * <p>
 * The class uses a 'stopLength' argument. Content before 'stopLength' is physically written to the disk,
 * whereas content at and after 'stopLength' is "virtual" content that is assumed to be all zero, but
 * whose size and position are still tracked. The virtual content is hence not actually stored aynwhere.
 * <p>
 * Only bytes before the 'stopLength' are actually read and written. Starting from 'stopLength':
 * <ul>
 * <li>Writes are not performed, but the virtual position and the virtual file size are updated
 * <li>Reads returns 0 (or -1 if the virtual file size has been reached)
 * </ul>
 * This works for testing huge WAVE audio files as long as:
 * <ul>
 * <li>'stopLength' is large enough to contain the initial headers and chunks
 * <li>The actual audio samples are all zero (at least after 'stopLength')
 * </ul>
 */
public class ZeroPaddedRandomAccessFile extends TSeekableDataOutputStream {
    
    private final long stopAtLength;
    private long length;
    private long position;

    
    /**
     * @param length virtual length. Specify 0 for writing a new file, it will be updated as writes are
     * performed. For an existing file, specify the exact virtual length
     */
    public ZeroPaddedRandomAccessFile(File file, long stopAtLength, long length) throws IOException {
        super(file);
        this.stopAtLength = stopAtLength;
        this.length = length;
        this.position = 0;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (pos <= stopAtLength)
            super.seek(pos);
        this.position = pos;
    }

    @Override
    public long getFilePointer() throws IOException {
        return this.position;
    }

    @Override
    public int read() throws IOException {
        if (position < stopAtLength) {
            // Read from physical file
            int result = super.read();
            if (result >= 0)
                position++;
            return result;
        } else if (position < length) {
            // Read from virtual file
            position++;
            return 0;
        } else {
            assert position >= length;
            // EOF
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position + len < stopAtLength) {
            // Read fully from physical file
            int result = super.read(b, off, len);
            position += result;
            return result;
        } else if (position < stopAtLength) {
            // Read partially from physical file, partially from virtual file
            int amount = (int) (stopAtLength - position);
            assert amount <= len;
            int result = super.read(b, off, amount);
            if (result > 0)
                position += result;
            if (result < amount)
                return result;
            assert result == amount;
            assert position == stopAtLength;
            if (result < len)
                return result + read(b, off + result, len - result); // This read will be virtual
            return result;
        } else if (position + len < length) {
            assert position >= stopAtLength;
            // Read fully from virtual file
            Arrays.fill(b, off, off + len, (byte) 0);
            position += len;
            return len;
        } else if (position < length) {
            // Read partially from virtual file
            int amount = (int) (length - position);
            Arrays.fill(b, off, off + amount, (byte) 0);
            position += amount;
            return amount;
        } else {
            assert position >= length;
            // EOF
            return -1;
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (position < stopAtLength) {
            // Write to physical file
            super.write(b);
            position++;
        } else {
            assert position >= stopAtLength;
            // Write to virtual file
            position++;
        }
        if (position > length)
            length = position;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (position + len < stopAtLength) {
            // Write fully to physical file
            super.write(b, off, len);
            position += len;
        } else if (position < stopAtLength) {
            // Write partially to physical file, partially to virtual file
            int amount = (int) (stopAtLength - position);
            assert amount <= len;
            super.write(b, off, amount);
            position += amount;
            assert position == stopAtLength;
            if (position > length)
                length = position;
            if (amount < len)
                write(b, off + amount, len - amount); // This write will be virtual
            return;
        } else {
            assert position >= stopAtLength;
            // Write fully to virtual file
            position += len;
        }
        if (position > length)
            length = position;
    }

    @Override
    public void setLength(long newLength) throws IOException {
        super.setLength(Math.min(newLength, stopAtLength));
        this.length = newLength;
    }

    @Override
    public long length() throws IOException {
        return this.length;
    }

}
