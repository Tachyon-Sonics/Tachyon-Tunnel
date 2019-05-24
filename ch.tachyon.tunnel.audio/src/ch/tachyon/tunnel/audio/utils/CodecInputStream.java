package ch.tachyon.tunnel.audio.utils;

import java.io.*;
import java.nio.charset.*;

/**
 * {@link InputStream} that wraps another {@link InputStream} and dynamically changes its {@link Charset}.
 */
public class CodecInputStream extends InputStream {

    private final static int BUFFER_SIZE = 1024;

    private final Charset dstCharset;
    private final Reader reader;

    private byte[] buffer;
    private int available;
    private int index;


    /**
     * Create a new {@link InputStream}
     * @param source the source input stream to read from
     * @param srcCharset the {@link Charset} of the source input stream
     * @param dstCharset the {@link Charset} of the created input stream
     */
    public CodecInputStream(InputStream source, Charset srcCharset, Charset dstCharset) {
        this.dstCharset = dstCharset;
        this.reader = new InputStreamReader(source, srcCharset);
    }

    @Override
    public int read() throws IOException {
        if (index >= available) {
            // Read and decode from source
            char[] chars = new char[BUFFER_SIZE];
            int amount = reader.read(chars);
            if (amount > 0) {
                // Encode and write to buffer
                ByteArrayOutputStream output = new ByteArrayOutputStream(BUFFER_SIZE);
                Writer writer = new OutputStreamWriter(output, dstCharset);
                writer.write(chars, 0, amount);
                writer.flush();
                // Set buffer and available size
                this.available = output.size();
                this.buffer = output.toByteArray();
                this.index = 0;
            }
        }
        if (index >= available)
            return -1;
        return buffer[index++];
    }

}
