package org.tritonus.sampled.file;

import java.io.DataInputStream;
import java.io.IOException;
import org.tritonus.share.sampled.file.TAudioFileReader;

class WaveGUID {
    
    public final static int GUID_SIZE = 16;

    public static final WaveGUID SUBTYPE_PCM = new WaveGUID(0x00000001, 0x0000, 0x0010,
            0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);

    public static final WaveGUID SUBTYPE_IEEE_FLOAT = new WaveGUID(0x00000003, 0x0000,
            0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    
    public static final WaveGUID SUBTYPE_ALAW = new WaveGUID(0x6, 0x0,
            0x10, 0x80, 0x0, 0x0, 0xaa, 0x0, 0x38, 0x9b, 0x71);
    
    public static final WaveGUID SUBTYPE_ULAW = new WaveGUID(0x7, 0x0,
            0x10, 0x80, 0x0, 0x0, 0xaa, 0x0, 0x38, 0x9b, 0x71);

    public static final WaveGUID SUBTYPE_ADPCM = new WaveGUID(17, 0x0,
            0x10, 0x80, 0x0, 0x0, 0xaa, 0x0, 0x38, 0x9b, 0x71);


    private long i1;
    private int s1;
    private int s2;
    private int x1;
    private int x2;
    private int x3;
    private int x4;
    private int x5;
    private int x6;
    private int x7;
    private int x8;

    private WaveGUID() {
    }

    WaveGUID(long i1, int s1, int s2, int x1, int x2, int x3, int x4,
            int x5, int x6, int x7, int x8) {
        this.i1 = i1;
        this.s1 = s1;
        this.s2 = s2;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        this.x4 = x4;
        this.x5 = x5;
        this.x6 = x6;
        this.x7 = x7;
        this.x8 = x8;
    }

    public static WaveGUID read(DataInputStream dis) throws IOException {
        WaveGUID d = new WaveGUID();
        d.i1 = TAudioFileReader.readLittleEndianInt(dis);
        d.s1 = TAudioFileReader.readLittleEndianShort(dis);
        d.s2 = TAudioFileReader.readLittleEndianShort(dis);
        d.x1 = dis.read();
        d.x2 = dis.read();
        d.x3 = dis.read();
        d.x4 = dis.read();
        d.x5 = dis.read();
        d.x6 = dis.read();
        d.x7 = dis.read();
        d.x8 = dis.read();
        return d;
    }
    
    @Override
    public int hashCode() {
        return (int) i1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WaveGUID))
            return false;
        WaveGUID t = (WaveGUID) obj;
        if (i1 != t.i1)
            return false;
        if (s1 != t.s1)
            return false;
        if (s2 != t.s2)
            return false;
        if (x1 != t.x1)
            return false;
        if (x2 != t.x2)
            return false;
        if (x3 != t.x3)
            return false;
        if (x4 != t.x4)
            return false;
        if (x5 != t.x5)
            return false;
        if (x6 != t.x6)
            return false;
        if (x7 != t.x7)
            return false;
        if (x8 != t.x8)
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (long value : new long[] {i1, s1, s2, x1, x2, x3, x4, x5, x6, x7, x8}) {
            if (result.length() > 0)
                result.append(", ");
            result.append("0x" + Long.toHexString(value));
        }
        return "[" + result.toString() + "]";
    }

}
