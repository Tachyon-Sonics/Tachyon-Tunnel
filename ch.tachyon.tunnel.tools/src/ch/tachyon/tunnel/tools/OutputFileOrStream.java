package ch.tachyon.tunnel.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;

public class OutputFileOrStream {
	
	private final File file;
	private final OutputStream stream;

	
	public OutputFileOrStream(String fileName) {
		if (fileName.equals("-")) {
			this.file = null;
			this.stream = new BufferedOutputStream(System.out);
		} else {
			this.file = new File(fileName);
			this.stream = null;
		}
	}
	
	public OutputFileOrStream(File file) {
		this.file = file;
		this.stream = null;
	}
	
	public OutputFileOrStream(OutputStream stream) {
		this.file = null;
		this.stream = new BufferedOutputStream(stream);
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
