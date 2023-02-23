package ch.tachyon.tritonus.test.util;

import java.io.File;
import java.io.IOException;


import ch.tachyon.tunnel.common.utils.Debug;

public class FfmpegAudioFileChecker {
	
	/**
	 * assert that the given audio file can be read fully by ffmpeg without any error (including recoverable errors).
	 * <p>
	 * ffmpeg must either be in the PATH, or its path must be specified using 
	 * <tt>-D{@link Ffmpeg#FFMPEG_PATH_PROPERTY ffmpeg.path}=&lt;path to ffmpeg.exe&gt;</tt>
	 * <p>
	 * If ffmpeg is not found, the check will fail, unless <tt>-Dffmpeg.path=disable</tt> is specified.
	 */
	public static void assertAudioFileSane(File audioFile) throws IOException {
        if (Ffmpeg.isFfmpegPresent()) {
        	// ffmpeg -v error -i file.avi -f null -
        	String error = Ffmpeg.run("-v", "error", "-i", audioFile.getAbsolutePath(), "-f", "null", "-");
        	assert error.isEmpty() : "ffmpeg found errors in the audio file \"" + audioFile + "\":\n" + error;
        } else if (Ffmpeg.isDisabled()) {
        	Debug.warn("ffmpeg check is disabled");
        } else {
        	assert false : "ffmpeg not found. Add it to the PATH or specify its path using -Dffmpeg.path=<path>";
        }
	}

}
