package ch.tachyon.tritonus.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ffmpeg {

    public static final String FFMPEG_PATH_PROPERTY = "ffmpeg.path";

    private static Boolean found;
    private static String ffmpeg;


    private static void lookup() {
        if (found == null) {
            if (isDisabled()) {
                found = false;
            } else {
                String pathStr = System.getProperty(FFMPEG_PATH_PROPERTY);
                if (pathStr != null)
                    ffmpeg = pathStr;
                else
                    ffmpeg = "ffmpeg";
                String[] command = new String[] { ffmpeg, "-version" };
                try {
                    int exitCode = CommandLine.runCommand(Arrays.asList(command));
                    found = (exitCode == 0);
                } catch (IOException ex) {
                    found = false;
                }
            }
        }
    }

    public static boolean isFfmpegPresent() {
        lookup();
        return found;
    }

    public static boolean isDisabled() {
        return "disabled".equals(System.getProperty(FFMPEG_PATH_PROPERTY));
    }

    public static String run(String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(ffmpeg);
        command.addAll(Arrays.asList(args));
        return CommandLine.runCommandReadError(command);
    }

}
