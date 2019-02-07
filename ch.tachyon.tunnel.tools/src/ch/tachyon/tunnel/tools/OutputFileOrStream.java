package ch.tachyon.tunnel.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;

public class OutputFileOrStream {
	
	private final File file;
	private final OutputStream stream;

	
	/**
	 * Create an {@link OutputFileOrStream} using the given file name.
	 * The special value "<tt>-</tt>" (without the quote) will create a new instance
	 * using the standard output {@link System#out}.
	 */
	public OutputFileOrStream(String fileName) {
		if (fileName.equals("-")) {
			this.file = null;
			this.stream = wrap(System.out);
		} else {
			this.file = new File(fileName);
			this.stream = null;
		}
	}
	
	private static OutputStream wrap(OutputStream stream) {
		if (stream instanceof BufferedOutputStream)
			return stream;
		return new BufferedOutputStream(stream);
	}
	
	public OutputFileOrStream(File file) {
		this.file = file;
		this.stream = null;
	}
	
	public OutputFileOrStream(OutputStream stream) {
		this.file = null;
		this.stream = wrap(stream);
	}

	public File getFile() {
		return file;
	}

	public OutputStream getStream() {
		return stream;
	}
	
	public boolean isFile() {
		return this.file != null;
	}
	
	public boolean alreadyExists() {
		return this.file != null && this.file.exists();
	}
	
	public String toString() {
		if (file != null)
			return file.getName();
		else
			return "stream";
	}
	
}
