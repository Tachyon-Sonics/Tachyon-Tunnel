package ch.tachyon.opus.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;

public class DecodedOpusAudioInputStream extends AudioInputStream {
    
    private final static boolean LOG_ENABLED = false;
    
    private final OpusFile opusFile;
    private final OpusDecoder opusDecoder;
    private final int nbChans;
    private int headToSkip;
    private byte[] currentOutput;
    private int currentOffset;
    private short[] decoded;
    

    public DecodedOpusAudioInputStream(InputStream stream, AudioFormat format, long length) throws IOException, OpusException {
        super(stream, format, length);
        OggFile oggFile = new OggFile(stream);
        this.opusFile = new OpusFile(oggFile);
        OpusInfo opusInfo = opusFile.getInfo();
        this.headToSkip = opusInfo.getPreSkip();
        this.opusDecoder = new OpusDecoder(opusInfo.getSampleRate(), opusInfo.getNumChannels());
        if (opusInfo.getNumChannels() != format.getChannels())
            throw new IllegalArgumentException("Channel count does not match");
        this.nbChans = format.getChannels();
    }

    @Override
    public int read() throws IOException {
        if (currentOutput == null || currentOffset >= currentOutput.length)
            decodeNextBlock();
        if (currentOutput == null || currentOffset >= currentOutput.length)
            return -1;
        return (currentOutput[currentOffset++] & 0xff);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = 0;
        if (len <= 0)
            return 0;
        while (len > 0) {
            if (currentOutput == null || currentOffset >= currentOutput.length)
                decodeNextBlock();
            if (currentOutput == null || currentOffset >= currentOutput.length)
                return (result <= 0 ? -1 : result);
            int amount = Math.min(len, currentOutput.length - currentOffset);
            System.arraycopy(currentOutput, currentOffset, b, off, amount);
            currentOffset += amount;
            off += amount;
            len -= amount;
            result += amount;
        }
        return result;
    }
    
    private void decodeNextBlock() throws IOException {
        OpusAudioData audioData = opusFile.getNextAudioPacket();
        if (audioData == null)
            return; // End of stream
        byte[] data = audioData.getData();
        if (data == null)
            return; // Should not happen... interpet as end of stream
        
        // Get packet info
        int nbSamples = audioData.getNumberOfSamples();
        int nbFrames = audioData.getNumberOfFrames();
        int arraySize = nbSamples * nbChans;
        if (decoded == null || decoded.length != arraySize)
            decoded = new short[arraySize];
        if (LOG_ENABLED)
            System.out.println("Audio data: " + data.length + "; frames: " + nbFrames + "; samples: " + nbSamples);
        
        // Decode
        int decodedSamples;
        try {
            decodedSamples = opusDecoder.decode(data, 0, data.length, decoded, 0, nbSamples, false);
            if (LOG_ENABLED)
                System.out.println("  Decoded: " + decodedSamples);
        } catch (OpusException ex) {
            throw new IOException(ex);
        }
        
        // Copy result
        int bufferSize = decodedSamples * nbChans * 2;
        if (currentOutput == null || currentOutput.length != bufferSize)
            currentOutput = new byte[bufferSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(currentOutput);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asShortBuffer().put(decoded, 0, decodedSamples * nbChans);
        if (headToSkip > 0) {
            int offset = Math.min(headToSkip, decodedSamples);
            currentOffset = offset * nbChans * 2; // 2 as offset is in byte
            headToSkip -= offset;
            if (currentOffset >= decoded.length) {
                System.err.println("WARNING: preSkip >= first frame size");
                // preSkip >= first frame size? this should not happen. Rather ignore
//                decodeNextBlock();
                headToSkip = 0;
                currentOffset = 0;
            }
        } else {
            currentOffset = 0;
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        opusFile.close();
        decoded = null;
        currentOutput = null;
    }

}
