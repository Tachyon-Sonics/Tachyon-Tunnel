package org.tritonus.sampled.file;

import java.util.HashMap;
import java.util.Map;

/**
 * "ds64" chunk in a RF64 or BW64 WAVE file. Holds lengths above 32-bit as 64-bit values
 */
public class Ds64Chunk {

    private final long riffSize;
    private final long dataSize;
    private final long sampleCount;
    private final Map<Integer, Long> lengthTable = new HashMap<>();

    
    public Ds64Chunk(long riffSize, long dataSize, long sampleCount) {
        this.riffSize = riffSize;
        this.dataSize = dataSize;
        this.sampleCount = sampleCount;
    }

    public long getRiffSize() {
        return riffSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    // Not really tested...
    public Map<Integer, Long> getLengthTable() {
        return lengthTable;
    }

}
