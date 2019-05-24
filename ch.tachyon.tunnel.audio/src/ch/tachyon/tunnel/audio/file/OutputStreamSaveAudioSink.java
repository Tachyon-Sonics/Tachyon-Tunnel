package ch.tachyon.tunnel.audio.file;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.sound.sampled.*;
import ch.tachyon.tunnel.audio.utils.*;
import ch.tachyon.tunnel.common.*;

/**
 * {@link IMultiChanAudioSink} implementation whose {@link IMultiChanAudioSink#writeSamples(float[][])} saves
 * the audio to an {@link OutputStream} in a given audio file format (WAVE, AIFF, etc).
 * <p>
 * This is pretty non-trivial because Java Audio requires an <i>InputStream</i> for saving audio, and hence
 * {@link AudioFileIO} relies on an {@link IMultiChanAudioSource} for reading the audio to save.
 * <p>
 * This class handles the boilerplate logic of converting from an {@link IMultiChanAudioSink} to an {@link IMultiChanAudioSource},
 * using a separate helper Thread, and in a way similar to {@link PipedInputStream} and {@link PipedOutputStream}.
 * <p>
 * Do not forget to call {@link #close()} to flush the underlying resources, such as terminating the helper thread.
 * <p>
 * Note: you should only use this class if you need to save audio using a {@link IMultiChanAudioSink}.
 * In general, the recommanded way is to rely on {@link IMultiChanAudioSource} only, and to use 
 * {@link AudioFileIO#saveAudioFile(IMultiChanAudioSource, OutputStream, AudioFileFormat, IProgressMonitor)}.
 */
public class OutputStreamSaveAudioSink implements IMultiChanAudioSink, Closeable {
    
    private final Thread writer;
    private final Exchanger<float[][]> exchanger = new Exchanger<float[][]>();
    private float[][] dstBuffer1;
    private float[][] dstBuffer2;
    private float[][] srcBuffer;
    private int srcOffset;
    private volatile boolean closed;
    private volatile boolean aborted;
    private IOException ioEx;
    private OperationAbortedException abortion;
    private AtomicInteger nbWriters = new AtomicInteger();
    
    
    /**
     * @param target the {@link OutputStream} into which to save the file. It can be a {@link FileOutputStream} but does not need to
     * @param saveFormat the format in which to save the file
     * @param progress optional - a progress monitor
     */
    public OutputStreamSaveAudioSink(final OutputStream target, final AudioFileFormat saveFormat, final IProgressMonitor progress) {
        this.writer = new Thread(new Runnable() {
            
            @Override
            public void run() {
                IMultiChanAudioSource pipedSource = new IMultiChanAudioSource() {
                    
                    @Override
                    public int readSamples(float[][] target) {
                        return OutputStreamSaveAudioSink.this.readSamples(target);
                    }
                    
                    @Override
                    public AudioFormat getAudioFormat() {
                        return saveFormat.getFormat();
                    }

                    @Override
                    public void close() {
                    }

                };
                try {
                    AudioFileIO.saveAudioFile(pipedSource, target, saveFormat, progress);
                } catch (IOException ex) {
                    ioEx = ex;
                } catch (OperationAbortedException ex) {
                    abortion = ex;
                }
                
                // Kill remaining writers
                aborted = true;
                while (nbWriters.get() > 0) {
                    try {
                        exchanger.exchange(null);
                    } catch (InterruptedException ex) {
                        // Ok, terminated
                        break;
                    }
                }
            }
        });
        writer.setName("OutputStreamSaveAudioSink");
        writer.start();
    }
    
    int readSamples(float[][] dstBuffer) {
        int length = dstBuffer[0].length;
        int dstOffset = 0;
        int result = 0;
        while (dstOffset < length) {
            if (srcBuffer == null || srcOffset >= srcBuffer[0].length) {
                // Retrieve next block
                if (closed)
                    return result;
                try {
                    srcBuffer = exchanger.exchange(null);
                } catch (InterruptedException ex) {
                    return result;
                }
                if (srcBuffer == null)
                    return result; // EOF
                srcOffset = 0;
            }
            int amount = Math.min(length - dstOffset, srcBuffer[0].length - srcOffset);
            for (int c = 0; c < dstBuffer.length; c++)
                System.arraycopy(srcBuffer[c], srcOffset, dstBuffer[c], dstOffset, amount);
            dstOffset += amount;
            srcOffset += amount;
            result += amount;
        }
        return result;
    }

    @Override
    public void writeSamples(float[][] samples) {
        nbWriters.incrementAndGet();
        try {
            if (aborted) {
                if (abortion != null)
                    throw abortion;
                else if (ioEx != null)
                    throw new RuntimeException(ioEx);
                else
                    return; // Early end-of-stream
            }
            
            if (dstBuffer1 == null || dstBuffer1.length != samples.length || dstBuffer1[0].length != samples[0].length)
                dstBuffer1 = new float[samples.length][samples[0].length];
            // Copy buffer as it might be overwritten before it has been saved
            for (int c = 0; c < samples.length; c++)
                System.arraycopy(samples[c], 0, dstBuffer1[c], 0, samples[c].length);
            // Send buffer to stream writer thread
            try {
                exchanger.exchange(dstBuffer1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            // Use double buffering so we can accept the next buffer while the previous one is being written
            float[][] temp = dstBuffer1;
            dstBuffer1 = dstBuffer2;
            dstBuffer2 = temp;
        } finally {
            nbWriters.decrementAndGet();
        }
    }
    
    @Override
    public void close() {
        closed = true;
        while (writer.isAlive()) {
            writer.interrupt();
            try {
                writer.join(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                break;
            }
        }
    }

}
