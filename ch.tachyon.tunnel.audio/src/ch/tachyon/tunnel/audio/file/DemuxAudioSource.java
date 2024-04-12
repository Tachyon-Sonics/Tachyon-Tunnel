package ch.tachyon.tunnel.audio.file;

import java.io.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;
import ch.tachyon.tunnel.common.utils.*;

/**
 * This class converts from and to {@link ISingleChanAudioSource}[] and {@link IMultiChanAudioSource}.
 * <p>
 * <b>Single channel to multi channel conversion</b>
 * <p>
 * To read from multiple {@link ISingleChanAudioSource} as a single {@link IMultiChanAudioSource}, use
 * the constructor {@link #DemuxAudioSource(ISingleChanAudioSource[])}, and then invoke the
 * {@link #readSamples(float[][])} method from the {@link IMultiChanAudioSource} interface that this class
 * implements.
 * <p>
 * <b>Multi channel to single channel conversion</b>
 * <p>
 * To read from one or more {@link ISingleChanAudioSource} corresponding to different channels of the same
 * {@link IMultiChanAudioSource}, use the {@link #DemuxAudioSource(IMultiChanAudioSource)} constructor.
 * Then use {@link #getSourceForChannel(int)} to get a {@link ISingleChanAudioSource} for every desired channel
 * and read from the returned {@link ISingleChanAudioSource} objects.
 * <p>
 * It is not necessary to read from every channel
 * and {@link #getSourceForChannel(int)} should not be used for channels you do not read.
 * <p>
 * When converting a {@link IMultiChanAudioSource} to one or more {@link ISingleChanAudioSource} using {@link #getSourceForChannel(int)},
 * this class tries to be very tolerant:
 * <ul>
 * <li>It is possible to {@link ISingleChanAudioSource#readSamples(float[])} multiple times from one channel, and then
 * multiple times from another channel; the reads do not need to be perfectly interleaved.
 * <li>It is also possible to read buffers of different size in different channels
 * <li>It is even possible to read from each channel concurrently by different threads, this class will synchronize as necessary.
 * </ul>
 * However, {@link #getSourceForChannel(int)} must be invoked for every channel that you want to read <i>before</i>
 * reading the first buffer. 
 * <p>
 * Note that if, at any time, N frames have been read from a given channel, and M frames have been read from
 * another channel, then abs(M - N) + k frames will be kept in memory (k is the size of the buffer being read the
 * last time the sign of N - M changed). Channels for which {@link #getSourceForChannel(int)} has not been invoked will not
 * be kept in memory though.
 * <p>
 * Hence, while is it possible to read from the channels in a way that it is not perfectly interleaved (especially when using different threads), you must ensure
 * that the number of frames read from the different channels do not differ more than what you accept can be kept in memory.
 * In particular, reading the whole stream in one channel and then the whole stream in another one is a scheme that should be avoided.
 * If you really need to go that way you should use {@link #getSourceForChannel(int)} for the first channel only, read the whole stream in that
 * channel, then recreate the {@link IMultiChanAudioSource} and {@link DemuxAudioSource}, and use {@link #getSourceForChannel(int)} for the next channel, and so on.
 * <p>
 */
public class DemuxAudioSource implements IMultiChanAudioSource, Closeable {
    
    private static final int MIN_MC_LENGTH = 128;
    private static final int DEFAULT_MC_LENGTH = 2048;
    private static final int MAX_MC_LENGTH = 32768;

    private final ISingleChanAudioSource[] audioSources;
    private final IMultiChanAudioSource mcAudioSource;
    private final ISingleChanAudioSource[] resultSources;
    private final List<Queue<float[]>> channelQueues;
    private final int[] channelIndex;
    private final BitSet activeChannels = new BitSet();
    private final Object lock = new Monitor("DemuxAudioSource");
    private boolean started = false;


    public DemuxAudioSource(ISingleChanAudioSource[] audioSources) {
        this.audioSources = audioSources;
        this.mcAudioSource = null;
        this.resultSources = null;
        this.channelQueues = null;
        this.channelIndex = null;
    }

    public DemuxAudioSource(IMultiChanAudioSource mcAudioSource) {
        this.mcAudioSource = mcAudioSource;
        this.audioSources = null;
        int nbChannels = mcAudioSource.getAudioFormat().getChannels();
        this.resultSources = new ISingleChanAudioSource[nbChannels];
        this.channelQueues = new ArrayList<>(nbChannels);
        for (int i = 0; i < nbChannels; i++) {
            Queue<float[]> emptyList = new LinkedList<>();
            channelQueues.add(emptyList);
        }
        this.channelIndex = new int[nbChannels];
    }

    @Override
    public AudioStreamFormat getAudioFormat() {
        if (mcAudioSource != null) {
            AudioFormat af = FormatUtils.fromStreamFormat(mcAudioSource.getAudioFormat());
            AudioFormat result = new AudioFormat(af.getSampleRate(), af.getSampleSizeInBits(), 1, !af.getEncoding().equals(Encoding.PCM_UNSIGNED), af.isBigEndian());
            return FormatUtils.toStreamFormat(result);
        } else {
            AudioFormat af = FormatUtils.fromStreamFormat(audioSources[0].getAudioFormat());
            AudioFormat result = new AudioFormat(af.getSampleRate(), af.getSampleSizeInBits(), audioSources.length, !af.getEncoding().equals(Encoding.PCM_UNSIGNED), af.isBigEndian());
            return FormatUtils.toStreamFormat(result);
        }
    }

    public int getNbChannels() {
        if (mcAudioSource != null)
            return mcAudioSource.getAudioFormat().getChannels();
        else
            return audioSources.length;
    }
    
    private void readNextMultiChanBlock(int length) {
        if (length < MIN_MC_LENGTH || length > MAX_MC_LENGTH)
            length = DEFAULT_MC_LENGTH;
        float[][] block = new float[channelIndex.length][length];
        int amount = mcAudioSource.readSamples(block);
        if (amount <= 0) {
            return;
        } else if (amount < length) {
            float[][] shrunk = new float[block.length][amount];
            for (int c = 0; c < block.length; c++)
                System.arraycopy(block[c], 0, shrunk[c], 0, amount);
            block = shrunk;
        }
        for (int c = 0; c < block.length; c++) {
            if (activeChannels.get(c))
                channelQueues.get(c).add(block[c]);
        }
    }

    int readSamples(int chan, float[] target) {
        if (audioSources != null)
            return audioSources[chan].readSamples(target);
        
        synchronized (lock) {
            started = true;
            int remaining = target.length;
            int offset = 0;
            Queue<float[]> queue = channelQueues.get(chan);
            while (remaining > 0) {
                if (queue.isEmpty()) {
                    assert channelIndex[chan] == 0;
                    readNextMultiChanBlock(remaining);
                }
                if (queue.isEmpty()) // End of stream
                    return offset;
                float[] block = queue.peek();
                int amount = Math.min(remaining, block.length - channelIndex[chan]);
                System.arraycopy(block, channelIndex[chan], target, offset, amount);
                offset += amount;
                remaining -= amount;
                channelIndex[chan] += amount;
                if (channelIndex[chan] >= block.length) {
                    queue.remove();
                    channelIndex[chan] = 0;
                }
            }
        }
        return target.length;
    }
    
    @Override
    public int readSamples(float[][] target) {
        if (mcAudioSource != null)
            return mcAudioSource.readSamples(target);
        int result = 0;
        for (int i = 0; i < audioSources.length; i++)
            result = audioSources[i].readSamples(target[i]);
        return result;
    }

    public int getNumberOfChannels() {
        if (mcAudioSource != null)
            return mcAudioSource.getAudioFormat().getChannels();
        else
            return audioSources.length;
    }
    
    public ISingleChanAudioSource getSourceForChannel(final int chan) {
        if (resultSources[chan] == null) {
            synchronized (lock) {
                if (started)
                    throw new IllegalStateException("Cannot create a single chan source after reading has started. You must create all the sources before reading");
                resultSources[chan] = new SingleChanSource(chan);
                activeChannels.set(chan);
            }
        }
        return resultSources[chan];
    }

    class SingleChanSource implements ISingleChanAudioSource, Closeable {
        
        private final int chan;

        private SingleChanSource(int chan) {
            this.chan = chan;
        }

        @Override
        public int readSamples(float[] target) {
            return DemuxAudioSource.this.readSamples(chan, target);
        }

        @Override
        public void close() {
            DemuxAudioSource.this.close();
        }

        @Override
        public AudioStreamFormat getAudioFormat() {
            return DemuxAudioSource.this.getAudioFormat();
        }
    }

    @Override
    public void close() {
        if (mcAudioSource != null)
            IOUtils.close(mcAudioSource);
        if (audioSources != null) {
            for (ISingleChanAudioSource source : audioSources)
                IOUtils.close(source);
        }
    }

}
