package ch.tachyon.tunnel.tools;

import java.io.*;

public class InputFileOrStream {

    private final File file;
    private final InputStream stream;

    /**
     * Create an {@link InputFileOrStream} using the given file name. The
     * special value "<tt>-</tt>" (without the quote) will create a new instance
     * using the standard input {@link System#in}.
     */
    public InputFileOrStream(String fileName) {
        if (fileName.equals("-")) {
            this.file = null;
            this.stream = wrap(System.in);
        } else {
            this.file = new File(fileName);
            this.stream = null;
        }
    }

    private static InputStream wrap(InputStream stream) {
        if (stream instanceof BufferedInputStream)
            return stream;
        return new BufferedInputStream(stream);
    }

    public InputFileOrStream(File file) {
        this.file = file;
        this.stream = null;
    }

    public InputFileOrStream(InputStream stream) {
        this.file = null;
        this.stream = wrap(stream);
    }

    public File getFile() {
        return file;
    }

    public InputStream getStream() {
        return stream;
    }

    public boolean isFile() {
        return this.file != null;
    }

    public boolean exists() {
        return this.stream != null || (this.file != null && this.file.isFile());
    }

    @Override
    public String toString() {
        if (file != null)
            return file.getName();
        else
            return "stream";
    }

}
