/*
 *	WaveAudioOutputStream.java
 *
 *	This file is part of Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 2000 by Florian Bomers <http://www.bomers.de>
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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.file.TAudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;

/**
 * AudioOutputStream for Wave files.
 *
 * @author Florian Bomers
 */

public class WaveAudioOutputStream extends TAudioOutputStream {

    // this constant is used for chunk lengths when the length is not known yet
    private static final int LENGTH_NOT_KNOWN = -1;
    private boolean junkChunkWritten = false;


    public WaveAudioOutputStream(AudioFormat audioFormat,
            long lLength,
            TDataOutputStream dataOutputStream) {
        // always do backpatching if the stream supports seeking, in case the
        // reported stream length is longer than the actual data
        super(audioFormat,
                lLength,
                dataOutputStream,
                dataOutputStream.supportsSeek());

        // double-check that we can write this audio format
        if (WaveTool.getFormatCode(getFormat()) == WaveTool.WAVE_FORMAT_UNSPECIFIED) {
            throw new IllegalArgumentException("Unknown encoding/format for WAVE file: " + audioFormat);
        }
        // WAVE requires unsigned 8-bit data
        requireSign8bit(false);
        // WAVE requires little endian
        requireEndianness(false);
        if (TDebug.TraceAudioOutputStream) {
            TDebug.out("Writing WAVE: " + audioFormat.getSampleSizeInBits() + " bits, " + audioFormat.getEncoding());
        }
    }

    @Override
    protected void writeHeader() throws IOException {
        if (TDebug.TraceAudioOutputStream) {
            TDebug.out("WaveAudioOutputStream.writeHeader()");
        }
        AudioFormat format = getFormat();
        int formatCode = WaveTool.getFormatCode(format);
        long lLength = getLength();
        int formatChunkAdd = 0;
        if (formatCode == WaveTool.WAVE_FORMAT_GSM610) {
            // space for extra fields
            formatChunkAdd += 2;
        }
        int dataOffset = WaveTool.DATA_OFFSET + formatChunkAdd;
        if (formatCode != WaveTool.WAVE_FORMAT_PCM && formatCode != WaveTool.WAVE_FORMAT_FLOAT) {
            // space for fact chunk
            dataOffset += WaveTool.MIN_FACT_CHUNK_LENGTH + WaveTool.CHUNK_HEADER_SIZE;
        }

        // chunks must be on word-boundaries
        long lDataChunkSize = lLength + (lLength % 2);
        if (lLength == AudioSystem.NOT_SPECIFIED) {
            lDataChunkSize = WaveTool.AUDIO_UNKNOWN_SIZE;
        }

        long RIFF_Size = lDataChunkSize + dataOffset - WaveTool.CHUNK_HEADER_SIZE;
        if (lLength != AudioSystem.NOT_SPECIFIED && RIFF_Size > 0xFFFFFFFFL) {
            // Will write a ds64 chunk
            RIFF_Size += WaveTool.MIN_DS64_CHUNK_LENGTH + WaveTool.CHUNK_HEADER_SIZE;
        } else if (lLength == AudioSystem.NOT_SPECIFIED || junkChunkWritten) {
            // Will write a JUNK chunk
            RIFF_Size += WaveTool.MIN_DS64_CHUNK_LENGTH + WaveTool.CHUNK_HEADER_SIZE;
        }
        if (lLength == AudioSystem.NOT_SPECIFIED) {
            RIFF_Size = WaveTool.AUDIO_UNKNOWN_SIZE;
        }

        TDataOutputStream dos = getDataOutputStream();

        // write RIFF container chunk
        if ((RIFF_Size > 0xFFFFFFFFL || lLength > 0xFFFFFFFFL) && (lLength != AudioSystem.NOT_SPECIFIED))
            dos.writeInt(WaveTool.WAVE_RF64_MAGIC);
        else
            dos.writeInt(WaveTool.WAVE_RIFF_MAGIC);
        dos.writeLittleEndian32(crop(RIFF_Size));
        dos.writeInt(WaveTool.WAVE_WAVE_MAGIC);

        if (lLength != AudioSystem.NOT_SPECIFIED && RIFF_Size > 0xFFFFFFFFL) {
            // Write a ds64 chunk
            dos.writeInt(WaveTool.WAVE_DS64_MAGIC);
            dos.writeLittleEndian32(WaveTool.MIN_DS64_CHUNK_LENGTH);
            dos.writeLittleEndian64(RIFF_Size);
            dos.writeLittleEndian64(lLength);
            dos.writeLittleEndian64(lLength / (long) format.getFrameSize());
            dos.writeLittleEndian32(0); // Table length: 0
        } else if (lLength == AudioSystem.NOT_SPECIFIED || junkChunkWritten) {
            // Write a JUNK chunk that we can patch later (or rewrite it)
            dos.writeInt(WaveTool.WAVE_JUNK_MAGIC);
            dos.writeLittleEndian32(WaveTool.MIN_DS64_CHUNK_LENGTH);
            dos.writeLittleEndian64(0);
            dos.writeLittleEndian64(0);
            dos.writeLittleEndian64(0);
            dos.writeLittleEndian32(0);
            junkChunkWritten = true;
        }

        // write fmt_ chunk
        int formatChunkSize = WaveTool.FMT_CHUNK_SIZE + formatChunkAdd;
        short sampleSizeInBits = (short) format.getSampleSizeInBits();
        int decodedSamplesPerBlock = 1;

        if (formatCode == WaveTool.WAVE_FORMAT_GSM610) {
            if (format.getFrameSize() == 33) {
                decodedSamplesPerBlock = 320;
            } else if (format.getFrameSize() == 65) {
                decodedSamplesPerBlock = 320;
            } else {
                // how to retrieve this value here ?
                decodedSamplesPerBlock = (int) (format.getFrameSize() * (320.0f / 65.0f));
            }
            sampleSizeInBits = 0; // MS standard
        }

        int avgBytesPerSec = ((int) format.getSampleRate()) / decodedSamplesPerBlock * format.getFrameSize();
        dos.writeInt(WaveTool.WAVE_FMT_MAGIC);
        dos.writeLittleEndian32(formatChunkSize);
        dos.writeLittleEndian16((short) formatCode); // wFormatTag
        dos.writeLittleEndian16((short) format.getChannels()); // nChannels
        dos.writeLittleEndian32((int) format.getSampleRate()); // nSamplesPerSec
        dos.writeLittleEndian32(avgBytesPerSec); // nAvgBytesPerSec
        dos.writeLittleEndian16((short) format.getFrameSize()); // nBlockalign
        dos.writeLittleEndian16(sampleSizeInBits); // wBitsPerSample
        dos.writeLittleEndian16((short) formatChunkAdd); // cbSize

        if (formatCode == WaveTool.WAVE_FORMAT_GSM610) {
            dos.writeLittleEndian16((short) decodedSamplesPerBlock); // wSamplesPerBlock
        }

        // write fact chunk

        if (formatCode != WaveTool.WAVE_FORMAT_PCM && formatCode != WaveTool.WAVE_FORMAT_FLOAT) {
            // write "fact" chunk: number of samples
            // todo: add this as an attribute or property
            // in AudioOutputStream or AudioInputStream
            long samples = 0;
            if (lLength != AudioSystem.NOT_SPECIFIED) {
                samples = lLength / format.getFrameSize() * decodedSamplesPerBlock;
            }
            // set as unknow of > 32 bit. A ds64 chunk will be added
            if (samples > 0xFFFFFFFFL) {
                samples = WaveTool.AUDIO_UNKNOWN_SIZE;
            }
            dos.writeInt(WaveTool.WAVE_FACT_MAGIC);
            dos.writeLittleEndian32(4);
            dos.writeLittleEndian32((int) (samples & 0xFFFFFFFFL));
        }

        // write header of data chunk
        dos.writeInt(WaveTool.WAVE_DATA_MAGIC);
        dos.writeLittleEndian32((lLength != AudioSystem.NOT_SPECIFIED) ? (crop(lLength)) : LENGTH_NOT_KNOWN);
    }
    
    private static int crop(long value) {
        if (value > 0xFFFFFFFFL)
            return -1;
        return (int) value;
    }

    @Override
    protected void patchHeader()
            throws IOException {
        TDataOutputStream tdos = getDataOutputStream();
        tdos.seek(0);
        setLengthFromCalculatedLength();
        writeHeader();
    }

    @Override
    public void close() throws IOException {
        long nBytesWritten = getCalculatedLength();

        if ((nBytesWritten % 2) == 1) {
            if (TDebug.TraceAudioOutputStream) {
                TDebug.out("WaveOutputStream.close(): adding padding byte");
            }
            // extra byte for to align on word boundaries
            TDataOutputStream tdos = getDataOutputStream();
            tdos.writeByte(0);
            // DON'T adjust calculated length !
        }

        super.close();
    }

}

/*** WaveAudioOutputStream.java ***/
