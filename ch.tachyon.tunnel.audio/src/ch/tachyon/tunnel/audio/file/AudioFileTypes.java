package ch.tachyon.tunnel.audio.file;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.spi.*;

import org.tritonus.sampled.file.*;
import org.tritonus.share.sampled.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.utils.*;

/**
 * Extension of {@link AudioSystem} for coping with file types.
 * <p>
 * This class looks for resources named <code>META-INF/services/org.corebounce.audio.file.FileReader</code>
 * in the classpath. Such files add the following information about an {@link AudioFileReader} spi:
 * <ul>
 * <li>The typical file extensions for the format (such as <code>.wav</code>, <code>.mp3</code>, etc)
 * <li>(Optional) A descriptive name for the format (such as <code>WAVE</code>)
 * </ul>
 * This class deals with:
 * <ul>
 * <li>{@link javax.sound.sampled.AudioFileFormat.Type}: the JavaSound's class to represent file types
 * <li><b>Extension</b> (String): typical file extensions such as ".wav", ".mp3", ".m4u", etc
 * <li><b>Type</b> (String): the human-readable name of the format, such as "WAVE", "MP3", "MP4/AAC"
 * </ul>
 * You generally do not need to use this class directly.
 * It is used by {@link AudioFileFilter} to create file chooser filters.
 * It is also used by {@link AudioFileIO} to recognize file types when loading and saving.
 * @see AudioFileFilter
 * @see AudioFileIO
 */
public class AudioFileTypes {

    private static final String SERVICE_RESOURCE = "META-INF/services/org.corebounce.audio.file.FileReader";

    private final static ThreadLocal<AudioFileTypes> instances = new ThreadLocal<AudioFileTypes>();
    private final static Lock readLock = new ReentrantLock(true); // Some providers are not thread safe

    // File extension -> Collection of class names
    private Map<String, Collection<String>> ext2fileReader = new HashMap<String, Collection<String>>();
    private Map<String, String> ext2Type = new LinkedHashMap<String, String>();
    private Map<String, Type> ext2writeType = new LinkedHashMap<String, Type>();
    private Set<String> threadUnsafeProviders = new HashSet<>(); // Class name


    private AudioFileTypes() {

    }

    public static synchronized AudioFileTypes instance() {
        AudioFileTypes result = instances.get();
        if (result == null) {
            result = new AudioFileTypes();
            instances.set(result);
            result.init();
        }
        return result;
    }

    protected void init() {
        // Add spi-based file reader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> metaInfs = cl.getResources(SERVICE_RESOURCE);
            while (metaInfs.hasMoreElements()) {
                URL metaInf = metaInfs.nextElement();
                final Map<Object, Object> props = new LinkedHashMap<Object, Object>();
                Properties propParser = new Properties() {

                    @Override
                    public synchronized Object put(Object key, Object value) {
                        props.put(key, value); // Copy in order-preserving map
                        return super.put(key, value);
                    }

                };
                try {
                    IOUtils.loadUtf8Properties(metaInf.openStream(), propParser);
                    boolean threadSafe = true;
                    Collection<String> foundClassNames = new ArrayList<>();
                    for (Object key0 : props.keySet()) {
                        String key = (String) key0;
                        if (key.startsWith("loader.")) {
                            String ext = key.substring("loader".length()).toLowerCase();
                            String className = (String) props.get(key);
                            if (className != null && !"".equals(className.trim()) && !"".equals(ext.substring(1).trim())) {
                                try {
                                    Class.forName(className, false, cl);
                                    Collection<String> classNames = ext2fileReader.get(ext);
                                    if (classNames == null) {
                                        classNames = new ArrayList<String>();
                                        ext2fileReader.put(ext, classNames);
                                    }
                                    classNames.add(className);
                                    foundClassNames.add(className);
                                } catch (Throwable ex) {
                                    Debug.warn("Cannot load JavaSound FileReader {0} for extension {1}", className, ext);
                                    Debug.warn(ex);
                                }
                            }
                        } else if (key.startsWith("type.")) {
                            String ext = key.substring("type".length()).toLowerCase();
                            String type = (String) props.get(key);
                            if (type != null && !type.trim().equals("") && !ext.substring(1).trim().equals(""))
                                ext2Type.put(ext, type);
                        } else if (key.equals("thread.safe")) {
                            threadSafe = !"false".equalsIgnoreCase((String) props.get(key));
                        }
                    }
                    if (!threadSafe)
                        threadUnsafeProviders.addAll(foundClassNames);
                } catch (IOException ex) {
                    Debug.warn(ex);
                } catch (Exception ex) {
                    // Try to continue with other formats even in grave exception
                    Debug.error(ex);
                }
            }
        } catch (IOException ex) {
            Debug.warn(ex);
        }

        // Complete ext2Types
        for (String ext : ext2fileReader.keySet()) {
            String type = ext2Type.get(ext);
            if (type == null) {
                type = ext.substring(1).toUpperCase();
                ext2Type.put(ext, type);
            }
        }

        // Remove types without file reader
        for (Iterator<String> iter = ext2Type.keySet().iterator(); iter.hasNext(); ) {
            String ext = iter.next();
            if (!ext2fileReader.containsKey(ext))
                iter.remove();
        }

        // Add file types supported by Java if not present
        if (!ext2Type.containsKey(".aiff"))
            ext2Type.put(".aiff", "AIFF/AIFC");
        if (!ext2Type.containsKey(".aifc"))
            ext2Type.put(".aifc", "AIFF/AIFC");
        if (!ext2Type.containsKey(".aif"))
            ext2Type.put(".aif", "AIFF/AIFC");
        if (!ext2Type.containsKey(".wav"))
            ext2Type.put(".wav", "WAVE");
        if (!ext2Type.containsKey(".au"))
            ext2Type.put(".au", "AU");

        // Build file extensions for writing
        Type[] writeTypes = AudioSystem.getAudioFileTypes();
        if (writeTypes != null) {
            for (Type type : writeTypes) {
                String extension = type.getExtension();
                if (extension != null) {
                    if (!extension.startsWith("."))
                        extension = "." + extension;
                    ext2writeType.put(extension, type);
                }
            }
        }

        // Add file types supported by Java if not present
        if (!ext2writeType.containsKey(".aiff"))
            ext2writeType.put(".aiff", Type.AIFF);
        if (!ext2writeType.containsKey(".aifc"))
            ext2writeType.put(".aifc", Type.AIFC);
        if (!ext2writeType.containsKey(".aif"))
            ext2writeType.put(".aif", Type.AIFF);
        if (!ext2writeType.containsKey(".wav"))
            ext2writeType.put(".wav", Type.WAVE);
        if (!ext2writeType.containsKey(".au"))
            ext2writeType.put(".au", Type.AU);
    }

    // File types support

    /**
     * Get the list of supported file audio file types for reading.
     * The results are not file extensions but rather small descriptions
     * such as AIFF/AIFC, RIFF/WAVE, etc.
     */
    public Collection<String> getSupportedReadTypes() {
        return new TreeSet<String>(ext2Type.values());
    }

    /**
     * Get the list of supported file audio file types for writing.
     * The results are not file extensions but rather small descriptions
     * such as AIFF/AIFC, RIFF/WAVE, etc.
     */
    public Collection<String> getSupportedWriteTypes() {
        Collection<String> result = new TreeSet<String>();
        for (String ext : ext2writeType.keySet()) {
            String type = ext2Type.get(ext);
            if (type == null)
                type = ext.substring(1).toUpperCase();
            result.add(type);
        }
        return result;
    }

    /**
     * Get the file extensions for a given file type.
     * The returned file extension starts with a dot.
     * @param type an audio file type as returned by {@link #getSupportedReadTypes()}
     */
    public Collection<String> getExtensionsForType(String type) {
        Collection<String> result = new ArrayList<String>();
        for (String ext : ext2Type.keySet()) {
            String extType = ext2Type.get(ext);
            if (extType.equals(type))
                result.add(ext);
        }
        return result;
    }

    /**
     * Get all the supported audio file extensions for reading
     */
    public Collection<String> getSupportedReadExtensions() {
        return Collections.unmodifiableSet(ext2Type.keySet());
    }

    /**
     * Get all the supported audio file extensions for writing
     */
    public Collection<String> getSupportedWriteExtensions() {
        return Collections.unmodifiableSet(ext2writeType.keySet());
    }

    /**
     * Get the best output encoding for the given input encoding.
     * The same encoding is returned if supported. Else an alternate one is chosen depending
     * on the bit depth. If the bit depth is not supported, null is returned.
     */
    public Encoding getEncodingFor(Encoding encoding, int bitDepth) {
        if (encoding != null) {
            if (encoding.equals(Encoding.PCM_SIGNED) && (bitDepth == 8 || bitDepth == 16 || bitDepth == 24 || bitDepth == 32))
                return Encoding.PCM_SIGNED;
            if (encoding.equals(Encoding.PCM_UNSIGNED) && bitDepth == 8)
                return Encoding.PCM_UNSIGNED;
            if (encoding.equals(Encodings.PCM_IEEE_FLOAT) && (bitDepth == 32 || bitDepth == 64))
                return Encodings.PCM_IEEE_FLOAT;
        }
        if (bitDepth == 8)
            return Encoding.PCM_UNSIGNED;
        if (bitDepth == 16 || bitDepth == 24)
            return Encoding.PCM_SIGNED;
        if (bitDepth == 32 || bitDepth == 64)
            return Encodings.PCM_IEEE_FLOAT;
        return null;
    }

    /**
     * Get the audio file type (a small descriptive name such as RIFF/WAVE) for the
     * given file name, based on the file name's extension
     */
    public String getAudioFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0)
            return null;
        String ext = fileName.substring(lastDot).toLowerCase();
        return ext2Type.get(ext);
    }

    public String getAudioFileType(Type fileType) {
        for (Map.Entry<String, Type> entry : ext2writeType.entrySet()) {
            if (entry.getValue().equals(fileType)) {
                String ext = entry.getKey();
                return ext2Type.get(ext);
            }
        }
        return null;
    }

    /**
     * Get a suitable {@link Type} that can be passed to
     * {@link AudioSystem#write(AudioInputStream, Type, OutputStream)} in order to save an audio file
     * of the given type. Returns <code>null</code> if the given type is not supported for saving.
     * @param type the file type, as returned by {@link #getSupportedReadTypes()}.
     */
    public Type getWritableType(String type) {
        for (String ext : getExtensionsForType(type)) {
            Type result = ext2writeType.get(ext);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * @param fileName only the file extension is relevant
     */
    public Type getWritableType4ext(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0)
            return null;
        String extension = fileName.substring(lastDot);
        return ext2writeType.get(extension);
    }

    public Type getPlatformDefaultType() {
        String osName = ("" + System.getProperty("os.name")).toLowerCase();
        if (osName.contains("windows"))
            return Type.WAVE;
        else if (osName.contains("mac"))
            return Type.AIFF;
        else
            return Type.AU;
    }

    // Opening file

    protected AudioFileReader getAudioFileReader(String fileName, AtomicBoolean threadSafe) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = fileName.substring(lastDot).toLowerCase();
            Collection<String> fileReaderClassNames = ext2fileReader.get(extension);
            if (fileReaderClassNames != null) {
                for (String fileReaderClassName : fileReaderClassNames) {
                    try {
                        Class<?> clazz = Class.forName(fileReaderClassName, true, classLoader);
                        AudioFileReader reader = (AudioFileReader) clazz.newInstance();
                        threadSafe.set(!threadUnsafeProviders.contains(fileReaderClassName));
                        return reader;
                    } catch (Exception ex) {
                        Debug.error(ex);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get an {@link AudioInputStream} for the given stream.
     * <p>
     * The decoder is choosen as follows
     * <ul>
     * <li>If a decoder is registered with the file extension, use it
     * <li>Use the improved search method
     * <li>Fallback to JavaSound's {@link AudioSystem#getAudioInputStream(InputStream)}
     * </ul>
     * @param fileName the file name. Only the file extension is relevant; it is used to select the correct
     * decoder if one is registered with the extension
     * @param fileInput the input stream to decode
     */
    AudioInputStream getAudioInputStream(String fileName, InputStream fileInput) throws IOException,
            UnsupportedAudioFileException {
        AtomicBoolean threadSafe = new AtomicBoolean();
        // Try with the reader corresponding to the extension
        AudioFileReader reader = getAudioFileReader(fileName, threadSafe);
        if (reader != null) {
            try {
                if (!threadSafe.get())
                    readLock.lock();
                return reader.getAudioInputStream(fileInput);
            } catch (IOException ex) {
                Debug.warn(ex);
            } catch (UnsupportedAudioFileException ex) {
                // Maybe the extension is wrong
                Debug.warn(ex);
            } finally {
                if (!threadSafe.get())
                    readLock.unlock();
            }
        }

        Debug.warn("Using JavaSound''s AudioSystem to open file {0}", fileName);
        // Failed. Fallback to JavaSound (try all readers)
        return AudioSystem.getAudioInputStream(fileInput);
    }

    /**
     * Replacement for {@link AudioSystem#write(AudioInputStream, Type, OutputStream)}. This version forces tritonus implementation
     * for WAVE/AIFF/AU files in order to support the PCM_IEEE_FLOAT encoding.
     * <p>
     * This method throws an {@link IllegalArgumentException} if the length of the input stream is unspecified
     * and the format is anything else than AU. Use {@link #write(AudioInputStream, Type, File)} instead.
     */
    static int write(AudioInputStream stream, AudioFileFormat.Type fileType, OutputStream out) throws IOException {
        if (fileType.equals(AudioFileFormat.Type.WAVE)) {
            // Force the use of tritonus
            WaveAudioFileWriter writer = new WaveAudioFileWriter();
            return writer.write(stream, fileType, out);
        } else if (fileType.equals(AudioFileFormat.Type.AIFF) || fileType.equals(AudioFileFormat.Type.AIFC)) {
            // Force the use of tritonus
        	AiffAudioFileWriter writer = new AiffAudioFileWriter();
        	return writer.write(stream, fileType, out);
        } else if (fileType.equals(AudioFileFormat.Type.AU)) {
        	// Force the use of tritonus
        	AuAudioFileWriter writer = new AuAudioFileWriter();
        	return writer.write(stream, fileType, out);
        } else {
            return AudioSystem.write(stream, fileType, out);
        }
    }
    
    /**
     * Replacement for {@link AudioSystem#write(AudioInputStream, Type, OutputStream)}. This version forces tritonus implementation
     * for WAVE/AIFF/AU files in order to support the PCM_IEEE_FLOAT encoding.
     * <p>
     * This method is always preferred over {@link #write(AudioInputStream, Type, OutputStream)} because it can set the audio file
     * length by seeking backwards and patching the header in case the given stream is of unspecified length.
     */
    static int write(AudioInputStream stream, AudioFileFormat.Type fileType, File outputFile) throws IOException {
        if (fileType.equals(AudioFileFormat.Type.WAVE)) {
            // Force the use of tritonus
            WaveAudioFileWriter writer = new WaveAudioFileWriter();
            return writer.write(stream, fileType, outputFile);
        } else if (fileType.equals(AudioFileFormat.Type.AIFF) || fileType.equals(AudioFileFormat.Type.AIFC)) {
            // Force the use of tritonus
            AiffAudioFileWriter writer = new AiffAudioFileWriter();
            return writer.write(stream, fileType, outputFile);
        } else if (fileType.equals(AudioFileFormat.Type.AU)) {
            // Force the use of tritonus
            AuAudioFileWriter writer = new AuAudioFileWriter();
            return writer.write(stream, fileType, outputFile);
        } else {
            return AudioSystem.write(stream, fileType, outputFile);
        }
    }

    /**
     * Get the audio file format of the given file
     * @param forceComputeLength Some audio format does not provide an exact frame length (mp3, m4a, etc), and hence if this flag is <tt>false</tt>,
     * {@link AudioFileFormat#getFrameLength()} will return {@link AudioSystem#NOT_SPECIFIED}. If this flag is <tt>true</tt> and no exact length
     * is available, this method will decode the entire file to compute the frame length. Be aware that this can be very time consuming.
     */
    public AudioFileFormat getAudioFileFormat(File file, boolean forceComputeLength) throws IOException, UnsupportedAudioFileException {
        InputStream fileInput = new RandomFileInputStream(file);
        AudioFileFormat result = getAudioFileFormat(file.getName(), fileInput);
        fileInput.close();
        if (result.getFrameLength() == AudioSystem.NOT_SPECIFIED) {
            Pair<AudioFormat, Integer> info = getFormatAndFrameLength(file);
            AudioFormat format = info.getFirst();
            int nbFrames = info.getSecond();
            result = new AudioFileFormat(result.getType(), format, nbFrames, result.properties());
        }
        return result;
    }

    private Pair<AudioFormat, Integer> getFormatAndFrameLength(File file) throws IOException, UnsupportedAudioFileException {
        final int bufferSize = 16384;
        IMultiChanAudioSource source = AudioFileIO.createAudioSource(file);
        int nbFrames = 0;
        float[][] buffer = new float[source.getAudioFormat().getChannels()][bufferSize];
        int read = source.readSamples(buffer);
        while (read > 0) {
            nbFrames += read;
            read = source.readSamples(buffer);
        }
        return new Pair<AudioFormat, Integer>(source.getAudioFormat(), nbFrames);
    }

    public AudioFileFormat getAudioFileFormat(String fileName, InputStream fileInput) throws IOException, UnsupportedAudioFileException {
        AtomicBoolean threadSafe = new AtomicBoolean();
        // Try with the reader corresponding to the extension
        AudioFileReader reader = getAudioFileReader(fileName, threadSafe);
        if (reader != null) {
            try {
                if (!threadSafe.get())
                    readLock.lock();
                return reader.getAudioFileFormat(fileInput);
            } catch (IOException ex) {
                Debug.warn(ex);
            } finally {
                if (!threadSafe.get())
                    readLock.unlock();
            }
        }

        // Failed. Fallback to JavaSound (try all readers)
        return AudioSystem.getAudioFileFormat(fileInput);
    }

}
