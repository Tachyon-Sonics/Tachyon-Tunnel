package ch.tachyon.tritonus.test.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class IffUtils {

    public static String readChunkId(InputStream input) throws IOException {
        long b0 = input.read();
        long b1 = input.read();
        long b2 = input.read();
        long b3 = input.read();
        if ((b0 | b1 | b2 | b3) < 0)
            throw new EOFException();
        return "" + (char) b0 + (char) b1 + (char) b2 + (char) b3;
    }

}
