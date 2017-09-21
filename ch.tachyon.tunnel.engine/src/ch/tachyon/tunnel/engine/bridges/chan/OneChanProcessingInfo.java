package ch.tachyon.tunnel.engine.bridges.chan;

import java.util.*;
import ch.tachyon.tunnel.host.*;

class OneChanProcessingInfo implements IProcessingInfo, IChanPartInfo {
    
    private final IProcessingInfo target;
    private final int chanIndex;
    private final int nbChans;
    private final BitSet channels;
    
    
    public OneChanProcessingInfo(IProcessingInfo target, int chanIndex, int nbChans) {
        this.target = target;
        this.chanIndex = chanIndex;
        this.nbChans = nbChans;
        this.channels = new BitSet();
        channels.set(chanIndex);
    }
    
    public int getNumberOfChannels() {
        return 1;
    }

    public float getSampleRate() {
        return target.getSampleRate();
    }

    public int getQuantization() {
        return target.getQuantization();
    }

    public int getMaxChunkLength(int pluginOptimalChunkLength) {
        return target.getMaxChunkLength(pluginOptimalChunkLength);
    }

    public int getHostPreferredChunkLength() {
        return target.getHostPreferredChunkLength();
    }

    public int negociateFixedChunkLength(int pluginOptimalChunkLength) {
        return target.negociateFixedChunkLength(pluginOptimalChunkLength);
    }

    public long getLength() {
        return target.getLength();
    }

    public int getChanIndex() {
        return chanIndex;
    }

    public BitSet getChannels() {
        return channels;
    }

    public int getTotalNbChannels() {
        return nbChans;
    }

}