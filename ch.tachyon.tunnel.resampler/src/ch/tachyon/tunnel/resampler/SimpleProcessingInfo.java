package ch.tachyon.tunnel.resampler;

import java.util.*;

import javax.sound.sampled.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;

/**
 * {@link IProcessingInfo} implementation that can be constructed using an {@link AudioFileFormat} or {@link AudioFormat},
 * and can be further be manipulated using getters and setters.
 */
public class SimpleProcessingInfo implements IProcessingInfo {
    
    private int numberOfChannels;
    private BitSet channels;
    private double sampleRate;
    private int quantization;
    private int hostPreferredChunkLength;
    private long length = -1;
    private IStorage storage;
    private IMtContext mtContext;

    public SimpleProcessingInfo() {

    }
    
    public SimpleProcessingInfo(AudioFormat format) {
        setNumberOfChannels(coalesce(format.getChannels(), 2));
        this.sampleRate = coalesce(format.getSampleRate(), 44100.0f);
        this.quantization = coalesce(format.getSampleSizeInBits(), 16);
    }
    
    public SimpleProcessingInfo(AudioFileFormat fileFormat) {
        this(fileFormat.getFormat());
        this.length = fileFormat.getFrameLength();
    }
    
    private static int coalesce(int value, int dflt) {
        if (value == AudioSystem.NOT_SPECIFIED)
            return dflt;
        return value;
    }

    private static float coalesce(float value, float dflt) {
        if (value == AudioSystem.NOT_SPECIFIED)
            return dflt;
        return value;
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
        if (this.channels == null) {
            this.channels = new BitSet();
            this.channels.set(0, numberOfChannels);
        }
    }
    
    public BitSet getChannels() {
        return channels;
    }

    public void setChannels(BitSet channels) {
        this.channels = channels;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    public int getQuantization() {
        return quantization;
    }

    public void setQuantization(int quantization) {
        this.quantization = quantization;
    }
    
    public int getHostPreferredChunkLength() {
        return hostPreferredChunkLength;
    }
    
    public void setHostPreferredChunkLength(int hostPreferredChunkLength) {
        this.hostPreferredChunkLength = hostPreferredChunkLength;
    }
    
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public IStorage getStorage() {
        return storage;
    }

    public void setStorage(IStorage storage) {
        this.storage = storage;
    }

    public IMtContext getMtContext() {
        return mtContext;
    }
    
    public void setMtContext(IMtContext mtContext) {
        this.mtContext = mtContext;
    }

    public int getMaxChunkLength(int pluginOptimalChunkLength) {
        return pluginOptimalChunkLength;
    }

    public int negociateFixedChunkLength(int pluginOptimalChunkLength) {
        return pluginOptimalChunkLength;
    }

}
