package ch.tachyon.tunnel.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

public class InputFileOrStream {
	
	private final File file;
	private final InputStream stream;

	
	public InputFileOrStream(String fileName) {
		if (fileName.equals("-")) {
			this.file = null;
			this.stream = new BufferedInputStream(System.in);
		} else {
			this.file = new File(fileName);
			this.stream = null;
		}
	}
	
	public InputFileOrStream(File file) {
		this.file = file;
		this.stream = null;
	}
	
	public InputFileOrStream(InputStream stream) {
		this.file = null;
		this.stream = new BufferedInputStream(stream);
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
	
	public String toString() {
		if (file != null)
			return file.getName();
		else
			return "stream";
	}
	
}
