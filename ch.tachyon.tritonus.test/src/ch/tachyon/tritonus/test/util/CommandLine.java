package ch.tachyon.tritonus.test.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CommandLine {

    private final static boolean LOG_ENABLED = false;


    public static int runCommand(List<String> command) throws IOException {
        return runCommand(null, command);
    }

    public static String runCommandReadError(List<String> command) throws IOException {
        return runCommandReadError(null, command);
    }

    public static int runCommand(File location, List<String> command) throws IOException {
        if (LOG_ENABLED)
            System.out.println("Running: " + command2string(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        if (location != null)
            builder.directory(location);
        builder.redirectOutput(Redirect.DISCARD);
        builder.redirectError(Redirect.INHERIT);
        Process process = builder.start();
        try {
            process.waitFor(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        int exitCode = process.exitValue();
        return exitCode;
    }

    public static String runCommandReadError(File location, List<String> command) throws IOException {
        if (LOG_ENABLED)
            System.out.println("Running: " + command2string(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        if (location != null)
            builder.directory(location);
        builder.redirectOutput(Redirect.INHERIT);
        builder.redirectError(Redirect.PIPE);
        Process process = builder.start();
        InputStream errorStream = new BufferedInputStream(process.getErrorStream());
        int data = errorStream.read();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (data >= 0) {
            System.out.print((char) data);
            if ((char) data == '\n')
                System.out.flush();
            buffer.write(data);
            data = errorStream.read();
        }
        buffer.flush();
        byte[] output = buffer.toByteArray();
        try {
            process.waitFor(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        return new String(output, StandardCharsets.UTF_8);
    }

    private static String command2string(List<String> command) {
        StringBuilder result = new StringBuilder();
        for (String item : command) {
            if (result.length() > 0)
                result.append(" ");
            boolean isFile = false;
            try {
                Path path = Paths.get(item);
                isFile = Files.isRegularFile(path) || Files.isDirectory(path);
            } catch (Exception ex) {
                // Ignore, just consider it's not a file
            }
            if (System.getProperty("os.name").toLowerCase().contains("windows") || !isFile) {
                if (item.contains(" "))
                    result.append("\"" + item + "\"");
                else
                    result.append(item);
            } else {
                result.append(item.replace(" ", "\\ "));
            }
        }
        return result.toString();
    }

}
