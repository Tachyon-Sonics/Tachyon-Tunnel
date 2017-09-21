package ch.tachyon.tunnel.engine.utils;

import java.io.*;

/**
 * {@link InputStream} that wraps another {@link InputStream} and dynamically changes its Charset.
 */
public class CodecInputStream extends InputStream {

    private final static int BUFFER_SIZE = 1024;

    private final String dstCharset;
    private final Reader reader;

    private byte[] buffer;
    private int available;
    private int index;


    /**
     * Create a new {@link InputStream}
     * @param source the source input stream to read from
     * @param srcCharset the Charset of the source input stream
     * @param dstCharset the Charset of the created input stream
     */
    public CodecInputStream(InputStream source, String srcCharset, String dstCharset) {
        this.dstCharset = dstCharset;
        try {
            this.reader = new InputStreamReader(source, srcCharset);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int read() throws IOException {
        if (index >= available) {
            // Read and decode next char buffer from source
            char[] chars = new char[BUFFER_SIZE];
            int amount = reader.read(chars);
            if (amount > 0) {
                // Encode and write to byte buffer
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
