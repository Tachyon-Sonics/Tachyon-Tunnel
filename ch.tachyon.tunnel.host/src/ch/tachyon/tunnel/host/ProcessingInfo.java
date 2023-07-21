package ch.tachyon.tunnel.host;

import java.util.*;

import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.host.effect.*;

/**
 * JavaBean implementation of {@link IProcessingInfo}.
 * <p>
 * As an alternative to implementing {@link IProcessingInfo} directly, a <code>ProcessingInfo</code>
 * object can be created.
 */
public class ProcessingInfo implements IProcessingInfo, Cloneable, java.io.Serializable {

    public final static int LENGTH_ANY = -2;
    public final static int LENGTH_UNKNOWN = -1;

    private int numberOfChannels;
    private BitSet channels;
    private double sampleRate;
    private int quantization;
    private long length;
    private int maxChunkLength;
    private int fixedChunkLength;


    public ProcessingInfo(int numberOfChannels, double sampleRate, int quantization, int chunkLength) {
        this(numberOfChannels, sampleRate, quantization, LENGTH_UNKNOWN, chunkLength, chunkLength);
    }

    /**
     * @param fixedChunkLength the fixed length of all the audio chunks that will be passed to the <code>process()</code> method.
     * The special value {@link #LENGTH_UNKNOWN} means that the size may vary. The special value {@link #LENGTH_ANY} means that
     * the length will be adjusted to the plugin's requested one, but no more than the specified maximum. The adjusted value can be
     * inquired after the call to {@link IEffect#startProcessing(IProcessingInfo)} using {@link #getFixedChunkLength()}.
     */
    public ProcessingInfo(int numberOfChannels, double sampleRate, int quantization, long length,
            int maxChunkLength, int fixedChunkLength) {
        this.numberOfChannels = numberOfChannels;
        this.sampleRate = sampleRate;
        this.quantization = quantization;
        this.length = length;
        this.maxChunkLength = maxChunkLength;
        this.fixedChunkLength = fixedChunkLength;
        this.channels = new BitSet();
        channels.set(0, numberOfChannels);
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
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

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getMaxChunkLength() {
        return maxChunkLength;
    }

    public int getMaxChunkLength(int pluginOptimalChunkLength) {
        return maxChunkLength;
    }

    public void setMaxChunkLength(int maxChunkLength) {
        this.maxChunkLength = maxChunkLength;
    }

    public int getFixedChunkLength() {
        return fixedChunkLength;
    }

    public int getHostPreferredChunkLength() {
        return Math.max(-1, fixedChunkLength);
    }

    public int negociateFixedChunkLength(int pluginOptimalChunkLength) {
        if (fixedChunkLength <= LENGTH_ANY)
            fixedChunkLength = Math.min(maxChunkLength, pluginOptimalChunkLength);
        return fixedChunkLength;
    }

    public void setFixedChunkLength(int fixedChunkLength) {
        this.fixedChunkLength = fixedChunkLength;
    }
    
    public static AudioStreamFormat toAudioFormat(IProcessingInfo info) {
        return new AudioStreamFormat(info.getSampleRate(), info.getNumberOfChannels(), info.getQuantization());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channels == null) ? 0 : channels.hashCode());
        result = prime * result + fixedChunkLength;
        result = prime * result + (int) (length ^ (length >>> 32));
        result = prime * result + maxChunkLength;
        result = prime * result + numberOfChannels;
        result = prime * result + quantization;
        long temp;
        temp = Double.doubleToLongBits(sampleRate);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessingInfo other = (ProcessingInfo) obj;
        if (channels == null) {
            if (other.channels != null)
                return false;
        } else if (!channels.equals(other.channels))
            return false;
        if (fixedChunkLength != other.fixedChunkLength)
            return false;
        if (length != other.length)
            return false;
        if (maxChunkLength != other.maxChunkLength)
            return false;
        if (numberOfChannels != other.numberOfChannels)
            return false;
        if (quantization != other.quantization)
            return false;
        if (Double.doubleToLongBits(sampleRate) != Double.doubleToLongBits(other.sampleRate))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProcessingInfo [numberOfChannels=" + numberOfChannels + ", sampleRate=" + sampleRate + ", length=" + length
                + ", maxChunkLength=" + maxChunkLength + ", fixedChunkLength=" + fixedChunkLength + "]";
    }

    @Override
    public ProcessingInfo clone() {
        try {
            return (ProcessingInfo) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

}
