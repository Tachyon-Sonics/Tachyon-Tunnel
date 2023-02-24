/*
 *	WaveAudioFileReader.java
 *
 *	This file is part of Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 1999,2000 by Florian Bomers <http://www.bomers.de>
 *  Copyright (c) 1999 by Matthias Pfisterer
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

package org.tritonus.sampled.file;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.file.RandomFileInputStream;
import org.tritonus.share.sampled.file.TAudioFileFormat;
import org.tritonus.share.sampled.file.TAudioFileReader;


/**
 * Class for reading wave files.
 *
 * @author Florian Bomers
 * @author Matthias Pfisterer
 */

public class WaveAudioFileReader extends TAudioFileReader {
    private static final int READ_LIMIT = 1000;


    public WaveAudioFileReader() {
        super(READ_LIMIT);
    }


    protected void advanceChunk(DataInputStream dis, long prevLength, long prevRead)
            throws IOException {
        if (prevLength > 0) {
            dis.skip(((prevLength + 1) & 0xFFFFFFFE) - prevRead);
        }
    }
    

    protected long findChunk(DataInputStream dis, int key, Ds64Chunk ds64Chunk)
            throws UnsupportedAudioFileException, IOException {
        // $$fb 1999-12-18: we should take care that we don't exceed
        // the mark of this stream. When we exceeded the mark and
        // we notice that we don't support this wave file,
        // other potential wave file readers have no chance.
        int thisKey;
        long chunkLength = 0;
        do {
            advanceChunk(dis, chunkLength, 0);
            try {
                thisKey = dis.readInt();
            } catch (IOException e) {
                if (TDebug.TraceAllExceptions) {
                    TDebug.out(e);
                }
                // $$fb: when we come here, we skipped past the end of the wave file
                // without finding the chunk.
                // IMHO, this is not an IOException, as there are incarnations
                // of WAVE files which store data in different chunks.
                // maybe we can find a nice description of the "required chunk" ?
                throw new UnsupportedAudioFileException(
                        "unsupported WAVE file: required chunk not found: " + chunkName(key));
            }
            chunkLength = (long) readLittleEndianInt(dis) & 0xFFFFFFFFL; // unsigned
            if (chunkLength == WaveTool.AUDIO_UNKNOWN_SIZE) {
                // Use length table of ds64Chunk if available
                if (ds64Chunk != null && ds64Chunk.getLengthTable().containsKey(thisKey)) {
                    chunkLength = ds64Chunk.getLengthTable().get(thisKey);
                } else if (ds64Chunk != null && thisKey == WaveTool.WAVE_DATA_MAGIC) {
                    chunkLength = ds64Chunk.getDataSize();
                } else {
                    throw new UnsupportedAudioFileException(
                            "unsupported WAVE file: chunk " + chunkName(thisKey) + " has unknown size.");
                }
            }
        } while (thisKey != key);
        return chunkLength;
    }

    protected Ds64Chunk readDs64Chunk(DataInputStream dis, long chunkLength) throws UnsupportedAudioFileException, IOException {
        int read = 0;
        if (chunkLength < WaveTool.MIN_DS64_SUPPORTED_CHUNK_LENGTH) {
            throw new UnsupportedAudioFileException(
                    "corrupt WAVE file: ds64 chunk is too small");
        }
        long riffSize = readLittleEndianLong(dis);
        read += 8;
        long dataSize = readLittleEndianLong(dis);
        read += 8;
        long sampleCount = readLittleEndianLong(dis);
        read += 8;
        Ds64Chunk ds64Chunk = new Ds64Chunk(riffSize, dataSize, sampleCount);
        if (read + 4 <= chunkLength) {
            int tableLength = readLittleEndianInt(dis);
            read += 4;
            for (int i = 0; i < tableLength; i++) {
                if (read + 12 > chunkLength)
                    break;
                int key = dis.readInt(); // Chunk name
                long size = readLittleEndianLong(dis); // 64-bit chunk size
                read += 12;
                ds64Chunk.getLengthTable().put(key, size);
            }
        }
        
        advanceChunk(dis, chunkLength, read);
        return ds64Chunk;
    }

    protected AudioFormat readFormatChunk(DataInputStream dis,
            long chunkLength) throws UnsupportedAudioFileException, IOException {
        String debugAdd = "";

        int read = WaveTool.MIN_FMT_CHUNK_LENGTH;

        if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH) {
            throw new UnsupportedAudioFileException(
                    "corrupt WAVE file: format chunk is too small");
        }

        short formatCode = readLittleEndianShort(dis);
        short channelCount = readLittleEndianShort(dis);
        if (channelCount <= 0) {
            throw new UnsupportedAudioFileException(
                    "corrupt WAVE file: number of channels must be positive");
        }

        int sampleRate = readLittleEndianInt(dis);
        if (sampleRate <= 0) {
            throw new UnsupportedAudioFileException(
                    "corrupt WAVE file: sample rate must be positive");
        }

        int avgBytesPerSecond = readLittleEndianInt(dis);
        int blockAlign = readLittleEndianShort(dis);

        AudioFormat.Encoding encoding;
        int sampleSizeInBits;
        int frameSize = 0;
        float frameRate = sampleRate;

        int cbSize = 0;
        Map<String, Object> properties = new HashMap<>();
        switch (formatCode) {
            case WaveTool.WAVE_FORMAT_PCM:
                if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH + 2) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: format chunk is too small");
                }
                sampleSizeInBits = readLittleEndianShort(dis);
                if (sampleSizeInBits <= 0) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: sample size must be positive");
                }
                encoding = (sampleSizeInBits <= 8) ? AudioFormat.Encoding.PCM_UNSIGNED : AudioFormat.Encoding.PCM_SIGNED;
                if (TDebug.TraceAudioFileReader) {
                    debugAdd += ", wBitsPerSample=" + sampleSizeInBits;
                }
                read += 2;
                break;
            case WaveTool.WAVE_FORMAT_FLOAT:
                sampleSizeInBits = 32;
                if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH + 2) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: format chunk is too small");
                }
                sampleSizeInBits = readLittleEndianShort(dis);
                read += 2;
                if (sampleSizeInBits != 32 && sampleSizeInBits != 64) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: sample size must be 32 or 64 for float encoding");
                }
                encoding = Encodings.PCM_IEEE_FLOAT;
                if (TDebug.TraceAudioFileReader) {
                    debugAdd += ", wBitsPerSample=" + sampleSizeInBits;
                }
                break;
            case WaveTool.WAVE_FORMAT_ALAW:
                sampleSizeInBits = 8;
                encoding = AudioFormat.Encoding.ALAW;
                break;
            case WaveTool.WAVE_FORMAT_ULAW:
                sampleSizeInBits = 8;
                encoding = AudioFormat.Encoding.ULAW;
                break;
            case WaveTool.WAVE_FORMAT_GSM610:
                if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH + 6) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: extra GSM bytes are missing");
                }
                sampleSizeInBits = readLittleEndianShort(dis); // sample Size (is 0 for GSM)
                cbSize = readLittleEndianShort(dis);
                if (cbSize < 2) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: extra GSM bytes are corrupt");
                }
                int decodedSamplesPerBlock = readLittleEndianShort(dis) & 0xFFFF; // unsigned
                if (TDebug.TraceAudioFileReader) {
                    debugAdd += ", wBitsPerSample=" + sampleSizeInBits
                            + ", cbSize=" + cbSize
                            + ", wSamplesPerBlock=" + decodedSamplesPerBlock;
                }
                sampleSizeInBits = AudioSystem.NOT_SPECIFIED;
                encoding = WaveTool.GSM0610;
                frameSize = blockAlign;
                frameRate = ((float) sampleRate) / ((float) decodedSamplesPerBlock);
                read += 6;
                break;

            case WaveTool.WAVE_FORMAT_IMA_ADPCM:
                if (chunkLength < WaveTool.MIN_FMT_CHUNK_LENGTH + 2) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: extra GSM bytes are missing");
                }
                sampleSizeInBits = readLittleEndianShort(dis);
                cbSize = readLittleEndianShort(dis);
                if (cbSize < 2) {
                    throw new UnsupportedAudioFileException(
                            "corrupt WAVE file: extra IMA ADPCM bytes are corrupt");
                }
                int samplesPerBlock = readLittleEndianShort(dis) & 0xFFFF; // unsigned
                if (TDebug.TraceAudioFileReader) {
                    debugAdd += ", wBitsPerSample=" + sampleSizeInBits
                            + ", cbSize=" + cbSize
                            + ", wSamplesPerBlock=" + samplesPerBlock;
                }
                sampleSizeInBits = AudioSystem.NOT_SPECIFIED;
                encoding = WaveTool.GSM0610;
                frameSize = blockAlign;
                frameRate = ((float) sampleRate) / ((float) samplesPerBlock);
                read += 6;
                break;

            case WaveTool.WAVE_FORMAT_EXTENSIBLE:
                sampleSizeInBits = readLittleEndianShort(dis);
                read += 2;
                cbSize = readLittleEndianShort(dis);
                read += 2;
                if (cbSize < 22)
                    throw new UnsupportedAudioFileException("corrupt WAVE file: invalid WAVE_EXTENSIBLE length");
                int validBitsPerSample = readLittleEndianShort(dis); // 2
                read += 2;
                int channelMask = readLittleEndianInt(dis); // 2 + 4 = 6
                read += 4;
                WaveGUID subFormat = WaveGUID.read(dis); // 6 + 16 = 22
                read += WaveGUID.GUID_SIZE;
                
                // Put additional info as properties
                properties.put("channelMask", channelMask);
                properties.put("validBitsPerSample", validBitsPerSample);
                
                // Get encoding
                if (subFormat.equals(WaveGUID.SUBTYPE_PCM)) {
                    if (sampleSizeInBits <= 8)
                        encoding = Encoding.PCM_UNSIGNED;
                    else
                        encoding = Encoding.PCM_SIGNED;
                } else if (subFormat.equals(WaveGUID.SUBTYPE_IEEE_FLOAT)) {
                    encoding = Encodings.PCM_IEEE_FLOAT;
                } else if (subFormat.equals(WaveGUID.SUBTYPE_ALAW)) {
                    encoding = Encodings.ALAW;
                } else if (subFormat.equals(WaveGUID.SUBTYPE_ULAW)) {
                    encoding = Encodings.ULAW;
                } else if (subFormat.equals(WaveGUID.SUBTYPE_ADPCM)) {
                    encoding = WaveTool.IMA_ADPCM;
                } else {
                    throw new UnsupportedAudioFileException(
                            "unsupported WAVE file: unknown subformat code " + subFormat);
                }
                break;
                
            default:
                throw new UnsupportedAudioFileException(
                        "unsupported WAVE file: unknown format code " + formatCode);
        }
        // if frameSize isn't set, calculate it (the default)
        if (frameSize == 0) {
            frameSize = calculateFrameSize(sampleSizeInBits, channelCount);
        }

        if (TDebug.TraceAudioFileReader) {
            TDebug.out("WaveAudioFileReader.readFormatChunk():");
            TDebug.out("  read values: wFormatTag=" + formatCode
                    + ", nChannels=" + channelCount
                    + ", nSamplesPerSec=" + sampleRate
                    + ", nAvgBytesPerSec=" + avgBytesPerSecond
                    + ", nBlockAlign==" + blockAlign
                    + debugAdd);
            TDebug.out("  constructed values: "
                    + "encoding=" + encoding
                    + ", sampleRate=" + ((float) sampleRate)
                    + ", sampleSizeInBits=" + sampleSizeInBits
                    + ", channels=" + channelCount
                    + ", frameSize=" + frameSize
                    + ", frameRate=" + frameRate);
        }

        // go to next chunk
        advanceChunk(dis, chunkLength, read);
        return new AudioFormat(
                encoding,
                sampleRate,
                sampleSizeInBits,
                channelCount,
                frameSize,
                frameRate,
                false,
                properties);
    }


    @Override
    protected AudioFileFormat getAudioFileFormat(InputStream inputStream, long lFileLengthInBytes)
            throws UnsupportedAudioFileException, IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int magic = dataInputStream.readInt();
        if (magic != WaveTool.WAVE_RIFF_MAGIC && magic != WaveTool.WAVE_RF64_MAGIC && magic != WaveTool.WAVE_BW64_MAGIC) {
            throw new UnsupportedAudioFileException(
                    "not a WAVE file: wrong header magic");
        }
        boolean hasDsChunk = (magic == WaveTool.WAVE_RF64_MAGIC || magic == WaveTool.WAVE_BW64_MAGIC);
        long totalLength = (long) readLittleEndianInt(dataInputStream) & 0xFFFFFFFFL; // unsigned
        magic = dataInputStream.readInt();
        if (magic != WaveTool.WAVE_WAVE_MAGIC) {
            throw new UnsupportedAudioFileException("not a WAVE file: wrong header magic");
        }

        Ds64Chunk ds64Chunk = null;
        if (hasDsChunk) {
            // search for "ds64" chunk
            long chunkLength = findChunk(dataInputStream, WaveTool.WAVE_DS64_MAGIC, null);
            ds64Chunk = readDs64Chunk(dataInputStream, chunkLength);
            if (totalLength == WaveTool.AUDIO_UNKNOWN_SIZE)
                totalLength = ds64Chunk.getRiffSize();// Use value of ds64 chunk if 32-bit value is unknown
        }

        // search for "fmt " chunk
        long chunkLength = findChunk(dataInputStream, WaveTool.WAVE_FMT_MAGIC, ds64Chunk);
        AudioFormat format = readFormatChunk(dataInputStream, chunkLength);

        // search for "data" chunk
        long dataChunkLength = findChunk(dataInputStream, WaveTool.WAVE_DATA_MAGIC, ds64Chunk);
        if (dataChunkLength == WaveTool.AUDIO_UNKNOWN_SIZE && ds64Chunk != null)
            dataChunkLength = ds64Chunk.getDataSize(); // Use value of ds64 chunk if 32-bit value is unknown

        long frameLength = (dataChunkLength == WaveTool.AUDIO_UNKNOWN_SIZE 
                ? WaveTool.AUDIO_UNKNOWN_SIZE 
                        : dataChunkLength / format.getFrameSize());
        if (format.getEncoding().equals(WaveTool.GSM0610)) {
            // todo: should not be necessary
            frameLength = dataChunkLength;
        }
        if (frameLength == WaveTool.AUDIO_UNKNOWN_SIZE && ds64Chunk != null)
            frameLength = ds64Chunk.getSampleCount();// Use value of ds64 chunk if 32-bit value is unknown

        if (TDebug.TraceAudioFileReader) {
            TDebug.out("WaveAudioFileReader.getAudioFileFormat(): total length: "
                    + totalLength + ", frame length = " + frameLength);
        }
        if (totalLength != WaveTool.AUDIO_UNKNOWN_SIZE)
            totalLength += WaveTool.CHUNK_HEADER_SIZE;
        
        if (ds64Chunk == null) {
            // Adjust any unkown length to 64-bit
            if (frameLength == WaveTool.AUDIO_UNKNOWN_SIZE)
                frameLength = -1;
            if (totalLength == WaveTool.AUDIO_UNKNOWN_SIZE)
                totalLength = -1;
        }
        
        if ((frameLength == -1L || totalLength == -1L)) {
            /*
             * Not sure this can happen: file has unknown length and does not have a ds64 chunk,
             * or the ds64 chunk also reports unknown length.
             */
            // Deduce length from file length
            if (inputStream instanceof FileInputStream) {
                FileInputStream fileInputStream = (FileInputStream) inputStream;
                totalLength = fileInputStream.getChannel().size();
                frameLength = (totalLength - fileInputStream.getChannel().position()) / format.getFrameSize();
            } else if (inputStream instanceof RandomFileInputStream) {
                RandomFileInputStream rfis = (RandomFileInputStream) inputStream;
                totalLength = rfis.getFileSize();
                frameLength = (totalLength - rfis.getFilePointer()) / format.getFrameSize();
            }
        }
        
        return new TAudioFileFormat(AudioFileFormat.Type.WAVE,
                format,
                frameLength,
                totalLength);
    }
}


/*** WaveAudioFileReader.java ***/

