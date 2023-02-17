package ch.tachyon.tunnel.audio.file;

import java.io.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.spi.*;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;

/**
 * Static utilities to read and write audio files into and from <code>float</code> arrays.
 * Encoding and decoding are handled automatically.
 * <p>
 * Reading and writing file is based on the {@link IMultiChanAudioSource} and {@link IMultiChanAudioSink}
 * interfaces.
 * <p>
 * This class supports the following extensions of Java's {@link AudioSystem} for reading audio files.
 * <p>
 * This class looks for resources named <code>META-INF/services/tachyon.tunnel.audio.file.FileReader</code>
 * in the classpath. Such files add the following information about an {@link AudioFileReader} spi:
 * <ul>
 * <li>The typical file extensions for the format (such as <code>.wav</code>, <code>.mp3</code>, etc)
 * <li>(Optional) A descriptive name for the format (such as <code>WAVE</code>)
 * </ul>
 * Using this extension, the class can directly find the correct {@link AudioFileReader} spi implementation(s)
 * based on the file extensions. This solves many problems that are frequently encountered with the
 * JavaSound's {@link AudioSystem} implementation when multiple providers are available:
 * <ul>
 * <li>A provider erroneously detects it can read a file, and reads garbage (some .mp3 providers for instance
 * might occasionnaly sucessfully read garbage from .mp4 files)
 * <li>A provider throws an IOException while parsing a file, blocking the whole process
 * <li>A provider scans the whole file to figure out it cannot read it, significantly slowing down the
 * whole process.
 * </ul>
 * See {@link AudioFileTypes} for more.
 * @see AudioFileTypes to detect audio file format information
 * @see DemuxAudioSource to extract a single channel
 */
public class AudioFileIO {
    
    /**
     * Create an audio source from the given file. Readind audio samples from the returned source will read bytes from the
     * file and automatically decode them into PCM audio.
     * @return a {@link IMultiChanAudioSource} from which the audio samples can be retrieved sequentially as <code>float[][]</code>
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @see AudioFileTypes#getAudioFileFormat(File, boolean) if you need file format information
     * @see DemuxAudioSource if you need to extract a single audio channel (or the unique channel as a {@link ISingleChanAudioSource} if the audio file is monophonic)
     */
    public static IMultiChanAudioSource createAudioSource(File file) throws IOException, UnsupportedAudioFileException {
        InputStream input = new RandomFileProgressInputStream(file);
        return createAudioSource(file.getName(), input);
    }
    
    public static IMultiChanAudioSource createAudioSource(File file, IProgressMonitor progress) throws IOException, UnsupportedAudioFileException {
        InputStream input = new RandomFileProgressInputStream(file, progress);
        return createAudioSource(file.getName(), input);
    }
    
    /**
     * Create an audio source from the given input stream. Readind audio samples from the returned source will read bytes from the
     * input stream and automatically decode them into PCM audio.
     * @param fileName the name of the file. Only the file extension (such as ".wav", ".mp3") is relevant, and is used to detect the audio file format
     * @param stream the input stream to read from. The input stream is not closed, and shall not be closed until all the samples have been read
     * from the returned object.
     * @return a {@link IMultiChanAudioSource} from which the audio samples can be retrieved sequentially as <code>float[][]</code>
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @see AudioFileTypes#getAudioFileFormat(String, InputStream) if you need file format information
     * @see DemuxAudioSource if you need to extract a single audio channel (or the unique channel as a {@link ISingleChanAudioSource} if the audio file is monophonic)
     */
    public static IMultiChanAudioSource createAudioSource(String fileName, InputStream stream) throws IOException, UnsupportedAudioFileException {
        AudioFileTypes aft = AudioFileTypes.instance();
        AudioInputStream audioInput = aft.getAudioInputStream(fileName, stream);
        return new AudioInputStreamSource(audioInput);
    }
    
    private static long getNbFrames(AudioFileFormat fileFormat) {
        if (fileFormat instanceof AudioFileFormatEx)
            return ((AudioFileFormatEx) fileFormat).getFrameLengthLong();
        return fileFormat.getFrameLength();
    }
    
    /**
     * Save an audio source to a file. This method will fully read the given source, encode using the given format and write the result to the
     * specified file.
     * @param audioSource the audio data
     * @param outputFile the destination file
     * @param saveFormat the output audio format
     * @param progress an optional {@link IProgressMonitor} implementation to monitor progress (and eventually interrupt the saving process)
     * @throws IOException 
     */
    public static void saveAudioFile(IMultiChanAudioSource audioSource, File outputFile, AudioFileFormat saveFormat, IProgressMonitor progress) throws IOException {
        AudioInputStream input = AudioSourceInputStream.createAudioInputStream(audioSource, saveFormat.getFormat(), progress, getNbFrames(saveFormat));
        try {
            AudioFileTypes.write(input, saveFormat.getType(), outputFile);
        } finally {
            IOUtils.close(audioSource);
            input.close();
        }
    }
    
    /**
     * Save an audio source to a stream. This method will fully read the given source, encode using the given format and write the result to the
     * specified stream.
     * <p>
     * This method will throw an {@link IllegalArgumentException} if the file format length is unspecified, the output stream is not a {@link FileOutputStream},
     * and the file format does not support unspecified length (actually only AU supports unspecified length because there is no API to ask for that feature).
     * If the file format length is unspecified, you
     * must use {@link #saveAudioFile(IMultiChanAudioSource, File, AudioFileFormat, IProgressMonitor)} instead, which is able to seek backward
     * and patch the header with the actual length before closing the file.
     * @param audioSource the audio data
     * @param target the output stream to write to. It is closed by this method.
     * @param saveFormat the output audio format
     * @param progress an optional {@link IProgressMonitor} implementation to monitor progress (and eventually interrupt the saving process)
     * @throws IOException 
     * @throws OperationAbortedException if the operation was aborted by the progress monitor
     */
    public static void saveAudioFile(IMultiChanAudioSource audioSource, OutputStream target, AudioFileFormat saveFormat, IProgressMonitor progress) throws IOException, OperationAbortedException {
        if (getNbFrames(saveFormat) == AudioSystem.NOT_SPECIFIED 
                && !(target instanceof FileOutputStream)
                && !saveFormat.getType().equals(Type.AU))
            throw new IllegalArgumentException("Writing to a stream with unspecified length is not allowed");
        AudioSourceInputStream source = AudioSourceInputStream.createAudioSourceInputStream(audioSource, saveFormat.getFormat(), progress, getNbFrames(saveFormat));
        AudioInputStream input = new AudioInputStream(source, saveFormat.getFormat(), getNbFrames(saveFormat));
        try {
            AudioFileTypes.write(input, saveFormat.getType(), target);
        } finally {
            IOUtils.close(audioSource);
            target.close();
            input.close();
        }
        OperationAbortedException abortion = source.aborted();
        if (abortion != null)
            throw abortion;
    }
    
    /**
     * Create an {@link InputStream} that read raw samples from the given {@link IMultiChanAudioSource} and convert the result into the specified audio file format.
     * <p>
     * Note: du to the way the Java Audio subsystem works, this method needs to create a separate Thread and use {@link PipedInputStream} / 
     * {@link PipedOutputStream}. On the other hand, this will allow multi-threading to be used when multiple processing are chained
     * @param audioSource the audio source to read from
     * @param saveFormat the audio file format
     * @param progress an optional progress monitor
     * @return a stream with the audio source encoded in the given audio file format
     */
    public static InputStream convertToFormat(IMultiChanAudioSource audioSource, final AudioFileFormat saveFormat, IProgressMonitor progress) throws IOException {
        if (getNbFrames(saveFormat) == AudioSystem.NOT_SPECIFIED && !saveFormat.getType().equals(Type.AU))
            throw new IllegalArgumentException("Writing to a stream with unspecified length is not allowed");
        AudioSourceInputStream source = AudioSourceInputStream.createAudioSourceInputStream(audioSource, saveFormat.getFormat(), progress, getNbFrames(saveFormat));
        final AudioInputStream input = new AudioInputStream(source, saveFormat.getFormat(), getNbFrames(saveFormat));
        final PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream pipeIn = new PipedInputStream(pipeOut);
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    AudioFileTypes.write(input, saveFormat.getType(), pipeOut);
                    pipeOut.flush();
                    pipeOut.close();
                } catch (IOException ex) {
                    Debug.error(ex);
                }
            }
        }, "AudioFileIO.pipe").start();
        // Use BufferedInputStream to support mark() - just enough to read the format's header and go back to the beginning of the stream
        return new BufferedInputStream(pipeIn, 4096);
    }
    
    /**
     * Close an object that might wrap a {@link Closeable}. For instance, some implementations of {@link IMultiChanAudioSource} wrap
     * an {@link InputStream}. Invoking this method on such an implementation will close the underlying input stream. Else it has no effect.
     * <p>
     * Implementation notes: this method just check if the object implements {@link Closeable}, and invokes {@link Closeable#close()} if yes.
     */
    public static void close(Object streamWrapper) throws IOException {
        if (streamWrapper instanceof Closeable)
            ((Closeable) streamWrapper).close();
    }

}
