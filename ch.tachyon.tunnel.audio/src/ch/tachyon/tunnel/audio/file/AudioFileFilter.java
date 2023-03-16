package ch.tachyon.tunnel.audio.file;

import java.io.*;
import java.util.*;

import javax.swing.filechooser.FileFilter;


public class AudioFileFilter extends FileFilter implements java.io.FileFilter {

    private static AudioFileFilter readInstance;
    private static AudioFileFilter writeInstance;

    private final Collection<String> extensions;
    private final String mainExtension;
    private final String label;


    /**
     * Get an instance that accept any recognized audio format for reading
     */
    public static AudioFileFilter readInstance() {
        if (readInstance == null)
            readInstance = new AudioFileFilter(false);
        return readInstance;
    }
    
    /**
     * @param additionalExtensions additional file extensions that are allowed in the "All supported audio files"
     * choice.
     */
    public static AudioFileFilter readInstance(Collection<String> additionalExtensions) {
        return new AudioFileFilter(false, additionalExtensions);
    }

    /**
     * Get an instance that accept any recognized audio format for writing
     */
    public static AudioFileFilter writeInstance() {
        if (writeInstance == null)
            writeInstance = new AudioFileFilter(true);
        return writeInstance;
    }

    /**
     * Get a list of instance. Each instance accepts one of the supported audio types
     */
    public static Collection<AudioFileFilter> individualInstances(boolean writing) {
        Collection<AudioFileFilter> result = new ArrayList<AudioFileFilter>();
        AudioFileTypes afs = AudioFileTypes.instance();
        Collection<String> types = (writing ? afs.getSupportedWriteTypes() : afs.getSupportedReadTypes());
        for (String fileType : types) {
            Collection<String> extensions = afs.getExtensionsForType(fileType);
            if (extensions.isEmpty())
                extensions.add("." + fileType.toLowerCase());
            String mainExtension = extensions.iterator().next();
            AudioFileFilter aff = new AudioFileFilter(label(fileType, extensions), extensions, mainExtension);
            result.add(aff);
        }
        return result;
    }

    protected AudioFileFilter(boolean writing) {
        AudioFileTypes afs = AudioFileTypes.instance();
        this.extensions = (writing ? afs.getSupportedWriteExtensions() : afs.getSupportedReadExtensions());
        this.mainExtension = null;
        this.label = "All Supported Audio Files";
    }

    protected AudioFileFilter(boolean writing, Collection<String> additionalExtensions) {
        AudioFileTypes afs = AudioFileTypes.instance();
        this.extensions = new HashSet<>();
        this.extensions.addAll(writing ? afs.getSupportedWriteExtensions() : afs.getSupportedReadExtensions());
        this.extensions.addAll(additionalExtensions);
        this.mainExtension = null;
        this.label = "All Supported Audio Files";
    }

    private AudioFileFilter(String label, Collection<String> extensions, String mainExtension) {
        this.extensions = extensions;
        this.mainExtension = mainExtension;
        this.label = label;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;
        String name = f.getName();
        for (String ext : this.extensions) {
            if (name.toLowerCase().endsWith(ext))
                return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return label;
    }

    public Collection<String> getExtensions() {
        return extensions;
    }

    /**
     * @return the main extension (starts with a dot)
     */
    public String getMainExtension() {
        return mainExtension;
    }

    public String getLabel() {
        return label;
    }

    private static String label(String fileType, Collection<String> extensions) {
        StringBuilder extList = new StringBuilder();
        for (String ext : extensions) {
            if (extList.length() > 0)
                extList.append(",");
            extList.append("*");
            extList.append(ext);
        }
        return fileType + " files (" + extList.toString() + ")";
    }

    @Override
    public String toString() {
        return label;
    }

}
